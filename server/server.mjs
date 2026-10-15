import http from 'node:http';
import { DatabaseSync } from 'node:sqlite';
import { randomBytes, randomInt, createHash, timingSafeEqual } from 'node:crypto';
import { mkdirSync } from 'node:fs';
import { resolve, dirname } from 'node:path';
import { pathToFileURL } from 'node:url';
import { split, ledger, weightedSplit } from './ledger.mjs';

const hash = v => createHash('sha256').update(v).digest('hex');
const id = () => randomBytes(16).toString('hex');
const fail = (status, message) => { const e = new Error(message); e.status = status; throw e; };
const clean = (v, max = 100) => typeof v === 'string' ? v.trim().slice(0, max) : '';
const json = v => JSON.parse(v);
function receipt(value) {
  if(!value)return '';
  if(typeof value!=='string'||value.length>1400000||!/^data:image\/jpeg;base64,[A-Za-z0-9+/]+=*$/.test(value))fail(400,'Choose a smaller JPEG receipt');
  const bytes=Buffer.from(value.split(',')[1],'base64');
  if(bytes[0]!==0xff||bytes[1]!==0xd8)fail(400,'Invalid receipt image');
  return value;
}
export function createApp(options = {}) {
  const env = options.env || process.env;
  const dev = env.DEV_AUTH === 'true';
  if (env.NODE_ENV === 'production' && dev) throw new Error('DEV_AUTH is forbidden in production');
  const path = options.dbPath || env.DB_PATH || resolve('data/chukta.sqlite');
  if (path !== ':memory:') mkdirSync(dirname(path), { recursive: true });
  const db = new DatabaseSync(path);
  db.exec(`
    PRAGMA journal_mode=WAL;
    CREATE TABLE IF NOT EXISTS users(id TEXT PRIMARY KEY, phone TEXT UNIQUE NOT NULL, name TEXT NOT NULL DEFAULT '', upi TEXT NOT NULL DEFAULT '');
    CREATE TABLE IF NOT EXISTS sessions(token TEXT PRIMARY KEY, userId TEXT NOT NULL, expires INTEGER NOT NULL);
    CREATE TABLE IF NOT EXISTS otp(phone TEXT PRIMARY KEY, digest TEXT NOT NULL, expires INTEGER NOT NULL, attempts INTEGER NOT NULL DEFAULT 0);
    CREATE TABLE IF NOT EXISTS trips(id TEXT PRIMARY KEY, data TEXT NOT NULL);
    CREATE TABLE IF NOT EXISTS limits(key TEXT PRIMARY KEY, count INTEGER NOT NULL, expires INTEGER NOT NULL);
  `);
  function limit(key, max, interval) {
    const now = Date.now(), row = db.prepare('SELECT * FROM limits WHERE key=?').get(key);
    if (!row || row.expires < now) {
      db.prepare('INSERT OR REPLACE INTO limits VALUES(?,?,?)').run(key, 1, now + interval);
      db.prepare('DELETE FROM limits WHERE expires < ?').run(now);
    } else {
      if (row.count >= max) fail(429, 'Too many attempts. Please wait and try again.');
      db.prepare('UPDATE limits SET count=count+1 WHERE key=?').run(key);
    }
  }
  async function twilio(resource, params) {
    if (!env.TWILIO_ACCOUNT_SID || !env.TWILIO_AUTH_TOKEN || !env.TWILIO_VERIFY_SERVICE_SID) fail(503, 'SMS provider is not configured');
    const response = await fetch('https://verify.twilio.com/v2/Services/' + env.TWILIO_VERIFY_SERVICE_SID + '/' + resource, {
      method: 'POST',
      headers: { Authorization: 'Basic ' + Buffer.from(env.TWILIO_ACCOUNT_SID + ':' + env.TWILIO_AUTH_TOKEN).toString('base64'), 'Content-Type': 'application/x-www-form-urlencoded' },
      body: new URLSearchParams(params),
      signal: AbortSignal.timeout(15000)
    });
    const body = await response.json();
    if (!response.ok) fail(502, 'SMS service could not process the request');
    return body;
  }
  const saveTrip = trip => { db.prepare('INSERT OR REPLACE INTO trips VALUES(?,?)').run(trip.id, JSON.stringify(trip)); return trip; };
  const getTrip = tripId => { const row = db.prepare('SELECT data FROM trips WHERE id=?').get(tripId); if (!row) fail(404, 'Trip not found'); return json(row.data); };
  const member = (trip, user) => { const m = trip.members.find(m => m.userId === user.id); if (!m) fail(403, 'You are not a member of this trip'); return m; };
  function transaction(fn) { db.exec('BEGIN IMMEDIATE'); try { const r = fn(); db.exec('COMMIT'); return r; } catch (e) { db.exec('ROLLBACK'); throw e; } }
  const publicTrip = t => ({ id: t.id, name: t.name, icon: t.icon, members: t.members.length, total: ledger(t).total,
    people: ledger(t).rows.filter(m => !m.userId).map(m => ({ id: m.id, name: m.name, balance: m.net })) });
  const server = http.createServer(async (req, res) => {
    res.setHeader('Content-Type', 'application/json; charset=utf-8');
    res.setHeader('Cache-Control', 'no-store');
    res.setHeader('X-Content-Type-Options', 'nosniff');
    res.setHeader('Access-Control-Allow-Origin', env.WEB_ORIGIN || 'http://localhost:8787');
    res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization');
    res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PATCH, DELETE, OPTIONS');
    if (req.method === 'OPTIONS') { res.writeHead(204); res.end(); return; }
    try {
      const url = new URL(req.url, 'http://localhost'), route = url.pathname;
      if (req.method === 'GET' && route === '/health') { res.end(JSON.stringify({ status: 'ok', mode: dev ? 'development' : 'production' })); return; }
      if(req.method==='GET' && /^\/join\/[a-f0-9]{32}$/.test(route)) {
        const code=route.split('/').pop();
        const trip=db.prepare('SELECT data FROM trips').all().map(r=>json(r.data)).find(t=>t.invite===code&&!t.closed);
        if(!trip)fail(404,'Invite not found or trip closed');
        const esc=s=>String(s).replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
        res.setHeader('Content-Type','text/html; charset=utf-8');
        res.end('<!doctype html><meta name="viewport" content="width=device-width,initial-scale=1"><title>Join '+esc(trip.name)+' · Chukta</title><style>body{margin:0;background:#fbf7f0;color:#1c1a17;font:16px system-ui}main{max-width:420px;margin:40px auto;padding:24px}h1{font-size:40px;line-height:1.1}header{padding:24px;background:#f7b500;border-radius:28px}a{display:block;background:#1c1a17;color:white;text-decoration:none;padding:18px;border-radius:18px;text-align:center;margin:24px 0}li{list-style:none;background:white;border:1px solid #e4dccf;padding:16px;margin:8px 0;border-radius:16px}ul{padding:0}code{overflow-wrap:anywhere}</style><main><header><b>chukta</b><h1>'+esc(trip.name)+'</h1><p>Hisaab saaf, dosti pakki.</p></header><p>Choose your name in Chukta to see your share and join your friends.</p><ul>'+trip.members.filter(m=>m.userId!=='kitty').map(m=>'<li>'+esc(m.name)+'</li>').join('')+'</ul><a href="chukta://invite/'+code+'">Open Chukta</a><p>Already installed? Tap Open Chukta. Or paste this invite code inside the app:</p><code>'+code+'</code></main>');
        return;
      }
      let body = {};
      if (['POST', 'PATCH'].includes(req.method)) {
        let buffer = ''; for await (const chunk of req) { buffer += chunk; if (Buffer.byteLength(buffer) > 1600000) fail(413, 'Request is too large'); }
        try { body = buffer ? json(buffer) : {}; } catch { fail(400, 'Invalid JSON'); }
        if (!body || typeof body !== 'object' || Array.isArray(body)) fail(400, 'Expected a JSON object');
      }
      let result;
      if (route === '/v1/auth/otp' && req.method === 'POST') {
        const phone = clean(body.phone, 20);
        if (!/^\+91[6-9]\d{9}$/.test(phone)) fail(400, 'Enter a valid Indian mobile number');
        limit('otp-ip:' + req.socket.remoteAddress, 20, 3600000);
        limit('otp:' + phone, 5, 3600000);
        const existing = db.prepare('SELECT * FROM otp WHERE phone=?').get(phone);
        if (existing && existing.expires - Date.now() > 240000) fail(429, 'Please wait 60 seconds before resending');
        const code = String(randomInt(100000, 1000000));
        if (!dev) await twilio('Verifications', { To: phone, Channel: 'sms' });
        db.prepare('INSERT OR REPLACE INTO otp VALUES(?,?,?,0)').run(phone, hash(code), Date.now() + 300000);
        result = { resendAfter: 60, ...(dev ? { devCode: code } : {}) };
      } else if (route === '/v1/auth/verify' && req.method === 'POST') {
        const phone = clean(body.phone, 20), code = clean(body.code, 6);
        limit('verify:' + req.socket.remoteAddress, 30, 600000);
        const challenge = db.prepare('SELECT * FROM otp WHERE phone=?').get(phone);
        if (!challenge || challenge.expires < Date.now() || challenge.attempts >= 5) fail(400, 'Code expired. Request a new code.');
        db.prepare('UPDATE otp SET attempts=attempts+1 WHERE phone=?').run(phone);
        const valid = /^\d{6}$/.test(code) && (dev
          ? timingSafeEqual(Buffer.from(challenge.digest), Buffer.from(hash(code)))
          : (await twilio('VerificationCheck', { To: phone, Code: code })).status === 'approved');
        if (!valid) fail(400, 'That code is not correct');
        const current = db.prepare('SELECT * FROM otp WHERE phone=?').get(phone);
        if (!current || current.digest !== challenge.digest) fail(400, 'Code already used');
        db.prepare('DELETE FROM otp WHERE phone=?').run(phone);
        let user = db.prepare('SELECT * FROM users WHERE phone=?').get(phone);
        if (!user) { user = { id: id(), phone, name: '', upi: '' }; db.prepare('INSERT INTO users VALUES(?,?,?,?)').run(user.id, phone, '', ''); }
        const token = id() + id();
        db.prepare('INSERT INTO sessions VALUES(?,?,?)').run(hash(token), user.id, Date.now() + 30 * 86400000);
        result = { token, user };
      } else if (/^\/v1\/invites\/[^/]+$/.test(route) && req.method === 'GET') {
        limit('invite:' + req.socket.remoteAddress, 50, 60000);
        const invite = route.split('/').pop();
        const trip = db.prepare('SELECT data FROM trips').all().map(r => json(r.data)).find(t => t.invite === invite && !t.closed);
        if (!trip) fail(404, 'Invite not found or trip closed');
        result = publicTrip(trip);
      } else {
        const token = req.headers.authorization?.replace(/^Bearer /, '');
        if (!token) fail(401, 'Please sign in');
        const session = db.prepare('SELECT * FROM sessions WHERE token=? AND expires>?').get(hash(token), Date.now());
        if (!session) fail(401, 'Session expired. Please sign in again.');
        const user = db.prepare('SELECT * FROM users WHERE id=?').get(session.userId);
        if (route === '/v1/me' && req.method === 'GET') result = user;
        else if (route === '/v1/me' && req.method === 'PATCH') {
          const name = clean(body.name, 60), upi = clean(body.upi, 100);
          if (!name) fail(400, 'Your name is required');
          if (upi && !/^[a-zA-Z0-9._-]{2,}@[a-zA-Z]{2,}$/.test(upi)) fail(400, 'Enter a valid UPI ID');
          db.prepare('UPDATE users SET name=?, upi=? WHERE id=?').run(name, upi, user.id);
          for (const row of db.prepare('SELECT data FROM trips').all()) { const t = json(row.data); const m = t.members.find(m => m.userId === user.id); if (m) { m.name = name; m.upi = upi; saveTrip(t); } }
          result = { ...user, name, upi };
        } else if (route === '/v1/logout' && req.method === 'POST') {
          db.prepare('DELETE FROM sessions WHERE token=?').run(hash(token)); result = { ok: true };
        } else if (route === '/v1/trips' && req.method === 'GET') {
          result = db.prepare('SELECT data FROM trips').all().map(r => json(r.data)).filter(t => t.members.some(m => m.userId === user.id));
        } else if (route === '/v1/trips' && req.method === 'POST') {
          const name = clean(body.name, 80); if (!name) fail(400, 'Give your trip a name');
          if (!Array.isArray(body.members) || body.members.length > 20) fail(400, 'Add up to 20 friends');
          const names = body.members.map(n => clean(n, 60)); if (names.some(n => !n)) fail(400, 'Each friend needs a name');
          const tripId=id();
          const members=[{ id: user.id, userId: user.id, name: user.name || 'You', upi: user.upi }, ...names.map(name => ({ id: id(), userId: '', name, upi: '' }))];
          if(body.kitty===true) members.push({id:'kitty-'+tripId,userId:'kitty',name:'Kitty',upi:''});
          result = saveTrip({ id: tripId, ownerId: user.id, name, icon: ['🏖️','🏔️','🎉','✈️'].includes(body.icon) ? body.icon : '🏖️', dates: clean(body.dates, 80), kitty: body.kitty === true, closed: false, invite: id(), members, expenses: [], settlements: [], contributions: [] });
        } else if (route === '/v1/join' && req.method === 'POST') {
          result = transaction(() => {
            const t = db.prepare('SELECT data FROM trips').all().map(r => json(r.data)).find(t => t.invite === clean(body.invite, 64) && !t.closed);
            if (!t) fail(404, 'Invite not found');
            if (t.members.some(m => m.userId === user.id)) return { trip: t, pending: false };
            const placeholder = t.members.find(m => m.id === body.memberId && !m.userId);
            if (!placeholder) fail(400, 'Choose your name on this trip, or ask the owner to add you.');
            t.joinRequests ||= [];
            if (!t.joinRequests.some(r => r.userId === user.id)) t.joinRequests.push({ userId: user.id, name: user.name || 'Friend', memberId: placeholder.id });
            saveTrip(t);
            return { trip: null, pending: true };
          });
        } else {
          const match = route.match(/^\/v1\/trips\/([^/]+)(?:\/(.*))?$/);
          if (!match) fail(404, 'Endpoint not found');
          const trip = getTrip(match[1]); member(trip, user);
          const action = match[2] || '';
          if (!action && req.method === 'GET') result = trip;
          else if (action === 'ledger' && req.method === 'GET') result = ledger(trip);
          else if (action === 'close' && req.method === 'POST') {
            if (trip.ownerId !== user.id) fail(403, 'Only the trip owner can close it');
            if (ledger(trip).transfers.length) fail(409, 'Settle outstanding balances before closing');
            trip.closed = true; result = saveTrip(trip);
          } else if (action === 'expenses' && req.method === 'POST') {
            if (trip.closed) fail(409, 'This trip is closed');
            const requestId = clean(body.id, 64);
            if (!/^[a-zA-Z0-9-]{8,64}$/.test(requestId)) fail(400, 'An expense request ID is required');
            if (trip.expenses.some(e => e.id === requestId)) result = trip;
            else {
              const amount = body.amount, title = clean(body.title, 100);
              if (!Number.isSafeInteger(amount) || amount <= 0 || amount > 1000000000) fail(400, 'Enter a valid amount in paise');
              if (!title || !trip.members.some(m => m.id === body.paidBy)) fail(400, 'Enter a title and valid payer');
              const people = trip.members.filter(m => m.userId !== 'kitty').map(m=>m.id);
              let shares;
              if (body.weights && Object.keys(body.weights).length) {
                if(Object.keys(body.weights).some(key=>!people.includes(key))) fail(400,'Invalid split member');
                try { shares=weightedSplit(amount,body.weights); } catch(e) { fail(400,e.message); }
              } else shares = split(amount, people);
              trip.expenses.push({ id: requestId, title, amount, paidBy: body.paidBy, category: ['Food','Stay','Travel','Fun'].includes(body.category) ? body.category : 'Food', date: /^\d{4}-\d{2}-\d{2}$/.test(body.date) ? body.date : new Date().toISOString().slice(0,10), shares, weights: body.weights || {}, receipt: receipt(body.receipt), addedBy: user.id });
              result = saveTrip(trip);
            }
          } else if (/^expenses\//.test(action) && req.method === 'PATCH') {
            if (trip.closed || trip.settlements.length) fail(409, 'Settled or closed expenses cannot be changed');
            const expense = trip.expenses.find(e=>e.id===action.split('/')[1]);
            if(!expense) fail(404,'Expense not found');
            if(expense.addedBy!==user.id&&trip.ownerId!==user.id) fail(403,'Only the author or owner can edit this expense');
            if(!Number.isSafeInteger(body.amount)||body.amount<=0||body.amount>1000000000||!clean(body.title)||!trip.members.some(m=>m.id===body.paidBy))fail(400,'Enter a valid expense');
            const people=trip.members.filter(m=>m.userId!=='kitty').map(m=>m.id);
            let shares;try {
              if(body.weights&&Object.keys(body.weights).length){if(Object.keys(body.weights).some(key=>!people.includes(key)))fail(400,'Invalid split member');shares=weightedSplit(body.amount,body.weights);}else shares=split(body.amount,people);
            }catch(e){fail(400,e.message);}
            Object.assign(expense,{title:clean(body.title),amount:body.amount,paidBy:body.paidBy,category:['Food','Stay','Travel','Fun'].includes(body.category)?body.category:'Food',date:/^\d{4}-\d{2}-\d{2}$/.test(body.date)?body.date:expense.date,shares,weights:body.weights||{},receipt:receipt(body.receipt)});
            result=saveTrip(trip);
          } else if (/^expenses\//.test(action) && req.method === 'DELETE') {
            if (trip.closed || trip.settlements.length) fail(409, 'Settled or closed expenses cannot be changed');
            const expense = trip.expenses.find(e => e.id === action.split('/')[1]);
            if (!expense) fail(404, 'Expense not found');
            if (expense.addedBy !== user.id && trip.ownerId !== user.id) fail(403, 'Only the author or owner can delete this expense');
            trip.expenses = trip.expenses.filter(e => e.id !== expense.id); result = saveTrip(trip);
          } else if (action === 'settlements' && req.method === 'POST') {
            if (trip.closed) fail(409, 'Trip is closed');
            const requestId = clean(body.id, 64);
            if (!/^[a-zA-Z0-9-]{8,64}$/.test(requestId)) fail(400, 'A payment request ID is required');
            if (trip.settlements.some(s => s.id === requestId)) result = trip;
            else {
              const own = member(trip, user), from = clean(body.from, 64), to = clean(body.to, 64);
              if (from !== own.id && trip.ownerId !== user.id) fail(403, 'You can only record your own payment');
              const due = ledger(trip).transfers.find(t => t.from === from && t.to === to);
              if (!due || !Number.isSafeInteger(body.amount) || body.amount <= 0 || body.amount > due.amount) fail(400, 'Payment exceeds the outstanding balance');
              if (!['cash','upi_manual'].includes(body.method)) fail(400, 'Choose cash or a manually confirmed UPI payment');
              const reference = clean(body.reference, 100);
              if (body.method === 'upi_manual' && !reference) fail(400, 'Enter the UPI reference after checking your payment');
              trip.settlements.push({ id: requestId, from, to, amount: body.amount, method: body.method, reference, recordedBy: user.id, verification: 'user_recorded', date: new Date().toISOString() });
              result = saveTrip(trip);
            }
          } else if (action === 'members' && req.method === 'POST') {
            if (trip.ownerId !== user.id) fail(403, 'Only the trip owner can edit members');
            const m = trip.members.find(m => m.id === body.memberId);
            if (!m) fail(404, 'Member not found');
            const upi = clean(body.upi, 100);
            if (!/^[a-zA-Z0-9._-]{2,}@[a-zA-Z]{2,}$/.test(upi)) fail(400, 'Enter a valid UPI ID');
            m.upi = upi; result = saveTrip(trip);
          } else if (action === 'approve' && req.method === 'POST') {
            if (trip.ownerId !== user.id) fail(403, 'Only the trip owner can approve members');
            const request = (trip.joinRequests || []).find(r => r.userId === body.userId);
            if (!request) fail(404, 'Join request not found');
            const placeholder = trip.members.find(m => m.id === request.memberId && !m.userId);
            const joining = db.prepare('SELECT * FROM users WHERE id=?').get(request.userId);
            if (!placeholder || !joining) fail(409, 'This name was already claimed');
            placeholder.userId = joining.id; placeholder.name = joining.name; placeholder.upi = joining.upi;
            trip.joinRequests = trip.joinRequests.filter(r => r.memberId !== placeholder.id && r.userId !== joining.id);
            result = saveTrip(trip);
          } else if (action === 'contributions' && req.method === 'POST') {
            if(!trip.kitty || trip.closed) fail(409,'This trip has no open kitty');
            const requestId=clean(body.id,64), own=member(trip,user);
            if(!/^[a-zA-Z0-9-]{8,64}$/.test(requestId)) fail(400,'A contribution request ID is required');
            const m=trip.members.find(m=>m.id===body.memberId && m.userId!=='kitty');
            if(!m || (m.id!==own.id && trip.ownerId!==user.id)) fail(403,'You can only record your own contribution');
            if(!Number.isSafeInteger(body.amount)||body.amount<=0||body.amount>1000000000) fail(400,'Enter a valid amount');
            trip.contributions ||= [];
            if(!trip.contributions.some(c=>c.id===requestId)) trip.contributions.push({id:requestId,memberId:m.id,amount:body.amount,date:new Date().toISOString()});
            result=saveTrip(trip);
          } else fail(404, 'Endpoint not found');
        }
      }
      res.end(JSON.stringify(result));
    } catch (e) { res.statusCode = e.status || 500; res.end(JSON.stringify({ error: e.status ? e.message : 'Something went wrong. Please retry.' })); if (!e.status) console.error(e); }
  });
  return { server, db };
}
if (process.argv[1] && import.meta.url === pathToFileURL(resolve(process.argv[1])).href) {
  const { server } = createApp();
  server.listen(Number(process.env.PORT || 8787), '0.0.0.0', () => console.log('Chukta API listening on ' + (process.env.PORT || 8787)));
}
