import test from 'node:test';
import assert from 'node:assert/strict';
import { createApp } from '../server.mjs';
test('authenticated trip, invite approval, idempotent expense and settlement', async () => {
  const {server,db}=createApp({dbPath:':memory:',env:{DEV_AUTH:'true'}});
  await new Promise(r=>server.listen(0,'127.0.0.1',r));
  const root='http://127.0.0.1:'+server.address().port;
  const req=async(path,token,body,method=body?'POST':'GET')=>{
    const res=await fetch(root+path,{method,headers:{'Content-Type':'application/json',...(token?{Authorization:'Bearer '+token}:{})},...(body?{body:JSON.stringify(body)}:{})});
    return {status:res.status,body:await res.json()};
  };
  try {
    const login=async(phone,name)=>{
      const challenge=await req('/v1/auth/otp',null,{phone});assert.equal(challenge.status,200);
      assert.equal((await req('/v1/auth/verify',null,{phone,code:'000000'})).status,400);
      const auth=await req('/v1/auth/verify',null,{phone,code:challenge.body.devCode});assert.equal(auth.status,200);
      assert.equal((await req('/v1/auth/verify',null,{phone,code:challenge.body.devCode})).status,400);
      await req('/v1/me',auth.body.token,{name,upi:''},'PATCH');return auth.body;
    };
    const owner=await login('+919876543210','Karan'), friend=await login('+919876543211','Aman');
    assert.equal((await req('/v1/trips')).status,401);
    let trip=(await req('/v1/trips',owner.token,{name:'Goa trip',icon:'🏖️',dates:'9–11 Oct',members:['Aman','Riya','Neha'],kitty:false})).body;
    assert.equal((await req('/v1/trips/'+trip.id,friend.token)).status,403);
    const expense={id:'test-expense-1',title:'Hotel',amount:1200000,paidBy:trip.members[1].id,category:'Stay',date:'2026-10-10'};
    trip=(await req('/v1/trips/'+trip.id+'/expenses',owner.token,expense)).body;
    const repeat=await req('/v1/trips/'+trip.id+'/expenses',owner.token,expense);assert.equal(repeat.body.expenses.length,1);
    const edited=await req('/v1/trips/'+trip.id+'/expenses/'+expense.id,owner.token,{...expense,title:'Hotel stay'},'PATCH');
    assert.equal(edited.body.expenses[0].title,'Hotel stay');
    assert.equal((await req('/v1/trips/'+trip.id+'/expenses',owner.token,{...expense,id:'test-expense-2',amount:-1})).status,400);
    const guest=await req('/v1/invites/'+trip.invite);assert.equal(guest.body.people.length,3);assert.equal(guest.body.people[0].balance,900000);
    const joined=await req('/v1/join',friend.token,{invite:trip.invite,memberId:trip.members[1].id});assert.equal(joined.body.pending,true);
    assert.equal((await req('/v1/trips/'+trip.id,friend.token)).status,403);
    trip=(await req('/v1/trips/'+trip.id+'/approve',owner.token,{userId:friend.user.id})).body;
    assert.equal((await req('/v1/trips/'+trip.id,friend.token)).status,200);
    const due=(await req('/v1/trips/'+trip.id+'/ledger',owner.token)).body.transfers.find(t=>t.from===owner.user.id);
    const payment={id:'test-payment-1',from:due.from,to:due.to,amount:10000,method:'cash',reference:''};
    trip=(await req('/v1/trips/'+trip.id+'/settlements',owner.token,payment)).body;
    assert.equal(trip.settlements[0].verification,'user_recorded');
    assert.equal((await req('/v1/trips/'+trip.id+'/settlements',owner.token,payment)).body.settlements.length,1);
    assert.equal((await req('/v1/trips/'+trip.id+'/settlements',owner.token,{...payment,id:'test-payment-2',amount:999999999})).status,400);
    assert.equal((await req('/v1/trips/'+trip.id+'/close',owner.token,{})).status,409);
    const kitty=(await req('/v1/trips',owner.token,{name:'Spiti',members:['Riya'],kitty:true})).body;
    assert.equal(kitty.members.length,3);
    const funding={id:'kitty-funding-1',memberId:owner.user.id,amount:100000};
    assert.equal((await req('/v1/trips/'+kitty.id+'/contributions',owner.token,funding)).status,200);
    assert.equal((await req('/v1/trips/'+kitty.id+'/contributions',owner.token,funding)).body.contributions.length,1);
    const kExpense={...expense,id:'kitty-expense-1',paidBy:'kitty-'+kitty.id,amount:60000,weights:{[owner.user.id]:1,[kitty.members[1].id]:1}};
    assert.equal((await req('/v1/trips/'+kitty.id+'/expenses',owner.token,kExpense)).status,200);
    const kLedger=(await req('/v1/trips/'+kitty.id+'/ledger',owner.token)).body;
    assert.deepEqual(kLedger.rows.map(r=>r.net),[70000,-30000,-40000]);
    await req('/v1/logout',owner.token,{});
    assert.equal((await req('/v1/me',owner.token)).status,401);
  } finally { await new Promise(r=>server.close(r));db.close(); }
});
test('production refuses development OTP and fails closed without SMS config', async()=>{
  assert.throws(()=>createApp({dbPath:':memory:',env:{DEV_AUTH:'true',NODE_ENV:'production'}}));
  const {server,db}=createApp({dbPath:':memory:',env:{NODE_ENV:'production'}});
  await new Promise(r=>server.listen(0,'127.0.0.1',r));
  const response=await fetch('http://127.0.0.1:'+server.address().port+'/v1/auth/otp',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({phone:'+919876543210'})});
  assert.equal(response.status,503);assert.equal((await response.json()).devCode,undefined);
  await new Promise(r=>server.close(r));db.close();
});
