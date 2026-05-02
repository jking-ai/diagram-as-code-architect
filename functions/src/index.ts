import { onRequest } from 'firebase-functions/v2/https';
import { defineSecret, defineString } from 'firebase-functions/params';
import express from 'express';
import cors from 'cors';
import { createProxyMiddleware } from 'http-proxy-middleware';
import type { ClientRequest, IncomingMessage } from 'http';

const apiKey = defineSecret('DIAGRAM_ARCHITECT_API_KEY');
const apiTarget = defineString('API_TARGET');

let app: express.Express | null = null;

function getApp(): express.Express {
  if (app) return app;

  app = express();
  app.use(cors({ origin: true }));

  app.use(
    '/',
    createProxyMiddleware({
      target: apiTarget.value(),
      changeOrigin: true,
      timeout: 120_000,
      on: {
        proxyReq: (proxyReq: ClientRequest, req: IncomingMessage) => {
          proxyReq.setHeader('X-API-Key', apiKey.value());

          // Firebase Functions v2 consumes the request body stream before it
          // reaches the proxy middleware. We use rawBody (the untouched Buffer
          // that Firebase provides) to re-write the original bytes.
          const firebaseReq = req as IncomingMessage & { rawBody?: Buffer; body?: unknown };
          if (firebaseReq.rawBody?.length) {
            proxyReq.setHeader('Content-Length', firebaseReq.rawBody.length);
            proxyReq.write(firebaseReq.rawBody);
          }
        },
      },
    })
  );

  return app;
}

export const diagramArchitectApi = onRequest(
  {
    region: 'us-central1',
    secrets: [apiKey],
    timeoutSeconds: 120,
    memory: '256MiB',
  },
  (req, res) => getApp()(req, res)
);
