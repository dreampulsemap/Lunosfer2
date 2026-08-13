const https = require('https');
const API_KEY = process.env.GEMINI_API_KEY;

const data = JSON.stringify({
  contents: [{ parts: [{ text: "Hello" }] }]
});

const options = {
  hostname: 'generativelanguage.googleapis.com',
  path: `/v1beta/models/gemini-3.5-flash:generateContent?key=${API_KEY}`,
  method: 'POST',
  headers: {
    'Content-Type': 'application/json',
    'Content-Length': data.length
  }
};

const req = https.request(options, (res) => {
  let result = '';
  res.on('data', (d) => { result += d; });
  res.on('end', () => {
    console.log("Status:", res.statusCode);
    console.log(result);
  });
});
req.write(data);
req.end();
