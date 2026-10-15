import test from 'node:test';
import assert from 'node:assert/strict';
import { split, ledger, weightedSplit } from '../ledger.mjs';
test('equal split keeps every paise', () => {
  assert.deepEqual(split(100000,['a','b','c']).map(s=>s.amount),[33334,33333,33333]);
  for(let amount=1;amount<1000;amount+=7) for(let n=1;n<12;n++) assert.equal(split(amount,Array.from({length:n},(_,i)=>String(i))).reduce((s,v)=>s+v.amount,0),amount);
});
test('weighted shares and kitty do not create or lose money',()=>{
  assert.deepEqual(weightedSplit(100000,{a:4,b:3}).map(s=>s.amount),[57143,42857]);
  assert.deepEqual(weightedSplit(101,{a:0,b:1}),[{memberId:'b',amount:101}]);
  const trip={id:'trip',members:[{id:'a'},{id:'b'},{id:'kitty-trip'}],contributions:[{memberId:'a',amount:100000}],expenses:[{paidBy:'kitty-trip',amount:60000,shares:split(60000,['a','b'])}],settlements:[]};
  assert.equal(ledger(trip).rows.reduce((s,r)=>s+r.net,0),0);
  assert.deepEqual(ledger(trip).rows.map(r=>r.net),[70000,-30000,-40000]);
});
test('Figma Goa case settles in the intended order', () => {
  const trip={members:['aman','riya','you','neha'].map(id=>({id})),expenses:[['aman',1200000],['riya',600000],['you',200000]].map(([paidBy,amount])=>({paidBy,amount,shares:split(amount,['aman','riya','you','neha'])})),settlements:[]};
  assert.deepEqual(ledger(trip).rows.map(r=>r.net),[700000,100000,-300000,-500000]);
  assert.deepEqual(ledger(trip).transfers,[{from:'neha',to:'aman',amount:500000},{from:'you',to:'aman',amount:200000},{from:'you',to:'riya',amount:100000}]);
  trip.settlements.push({from:'you',to:'aman',amount:100000});
  assert.equal(ledger(trip).rows.reduce((sum,r)=>sum+r.net,0),0);
  assert.equal(ledger(trip).rows.find(r=>r.id==='you').net,-200000);
});
