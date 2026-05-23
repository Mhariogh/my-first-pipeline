const http = require('http');
const { Pool } = require('pg');
const redis = require('redis');

const db = new Pool({ connectionString: process.env.DATABASE_URL });
const cache = redis.createClient({ url: process.env.REDIS_URL });
cache.connect().catch(console.error);

const server = http.createServer(async (req, res) => {
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Content-Type', 'application/json');

  if (req.url === '/health') {
    res.writeHead(200);
    res.end(JSON.stringify({ status: 'ok', service: 'backend-api' }));
  } else if (req.url === '/api/data') {
    try {
      const cached = await cache.get('data');
      if (cached) {
        res.writeHead(200);
        res.end(JSON.stringify({ source: 'cache', data: JSON.parse(cached) }));
        return;
      }
      const result = await db.query('SELECT NOW() as time, version() as version');
      await cache.setEx('data', 30, JSON.stringify(result.rows[0]));
      res.writeHead(200);
      res.end(JSON.stringify({ source: 'database', data: result.rows[0] }));
    } catch (err) {
      res.writeHead(500);
      res.end(JSON.stringify({ error: err.message }));
    }
  } else {
    res.writeHead(404);
    res.end(JSON.stringify({ error: 'Not found' }));
  }
});

server.listen(3000, () => console.log('Backend API running on port 3000'));
