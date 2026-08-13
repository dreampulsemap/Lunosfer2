const fetch = require('node-fetch');
async function test() {
  const url = "https://lunosfer.com/api/goals/list?mode=own&page=0";
  console.log("Fetching", url);
  try {
    const res = await fetch(url);
    console.log(res.status);
    const json = await res.json();
    console.log(Object.keys(json));
  } catch (e) {
    console.error(e);
  }
}
test();
