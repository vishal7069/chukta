export function split(amount, members) {
  if (!Number.isSafeInteger(amount) || amount <= 0 || !members.length || new Set(members).size !== members.length) throw new Error('Invalid split');
  const base = Math.floor(amount / members.length), remainder = amount % members.length;
  return members.map((memberId, i) => ({ memberId, amount: base + (i < remainder ? 1 : 0) }));
}
export function ledger(trip) {
  const rows = trip.members.map(m => ({ ...m, paid: 0, share: 0, net: 0 }));
  const byId = new Map(rows.map(m => [m.id, m]));
  for (const e of trip.expenses) {
    byId.get(e.paidBy).paid += e.amount;
    for (const s of e.shares) byId.get(s.memberId).share += s.amount;
  }
  for (const c of trip.contributions || []) {
    byId.get(c.memberId).paid += c.amount;
    byId.get('kitty-' + trip.id).paid -= c.amount;
  }
  for (const r of rows) r.net = r.paid - r.share;
  for (const s of trip.settlements) {
    byId.get(s.from).net += s.amount;
    byId.get(s.to).net -= s.amount;
  }
  const debtors = rows.filter(m => m.net < 0).sort((a,b) => a.net - b.net).map(m => ({ id: m.id, left: -m.net }));
  const creditors = rows.filter(m => m.net > 0).sort((a,b) => b.net - a.net).map(m => ({ id: m.id, left: m.net }));
  const transfers = [];
  for (const d of debtors) for (const c of creditors) {
    const amount = Math.min(d.left, c.left);
    if (amount > 0) { transfers.push({ from: d.id, to: c.id, amount }); d.left -= amount; c.left -= amount; }
  }
  return { rows, transfers, total: trip.expenses.reduce((sum, e) => sum + e.amount, 0) };
}
export function weightedSplit(amount, weights) {
  const entries = Object.entries(weights);
  if (!entries.length || entries.some(([,w])=>!Number.isSafeInteger(w)||w<0||w>1000)) throw new Error('Invalid shares');
  const sum = entries.reduce((s,[,w])=>s+w,0); if (!sum) throw new Error('Choose at least one person');
  const shares = entries.map(([memberId,w],i)=>({memberId,amount:Math.floor(amount*w/sum),fraction:amount*w%sum,index:i}));
  let remaining = amount - shares.reduce((s,v)=>s+v.amount,0);
  for (const s of [...shares].sort((a,b)=>b.fraction-a.fraction||a.index-b.index)) if(remaining-->0) s.amount++;
  return shares.filter(s=>s.amount>0).map(({memberId,amount})=>({memberId,amount}));
}
