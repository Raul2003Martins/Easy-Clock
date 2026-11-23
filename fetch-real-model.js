const https = require('https');
const http = require('http');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const destDir = path.join(__dirname, 'facial_prototype', 'models');
if (!fs.existsSync(destDir)) fs.mkdirSync(destDir, { recursive: true });

const destPath = path.join(destDir, 'mobilefacenet.tflite');

const candidateUrls = [
  'https://huggingface.co/qualcomm/MobileFaceNet/resolve/main/MobileFaceNet.tflite?download=true',
  'https://huggingface.co/qualcomm/MobileFaceNet/resolve/main/MobileFaceNet.tflite',
  'https://raw.githubusercontent.com/renesas/ruhmi-model-zoo/main/vision/face_recognition/mobilefacenet/mobilefacenet.tflite',
  'https://github.com/renesas/ruhmi-model-zoo/raw/main/vision/face_recognition/mobilefacenet/mobilefacenet.tflite',
  'https://github.com/serengil/deepface_models/releases/download/v1.0/mobilefacenet.tflite',
  'https://raw.githubusercontent.com/shubham0204/Face-Recognition-TFLite-Android/main/app/src/main/assets/mobilefacenet.tflite',
  'https://raw.githubusercontent.com/syed-zain-ali/Face-Recognition-Android-App/master/app/src/main/assets/mobilefacenet.tflite',
  'https://raw.githubusercontent.com/tiancaijie/MobileFaceNet-Android/master/app/src/main/assets/mobilefacenet.tflite'
];

function downloadUrl(url, dest, redirectCount = 0) {
  if (redirectCount > 5) return Promise.reject(new Error("Muitos redirecionamentos"));

  return new Promise((resolve, reject) => {
    const client = url.startsWith('https') ? https : http;
    const req = client.get(url, { headers: { 'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)' } }, (res) => {
      if (res.statusCode === 301 || res.statusCode === 302 || res.statusCode === 307 || res.statusCode === 308) {
        const redirectUrl = res.headers.location;
        console.log(`[REDIRECIONAMENTO ${res.statusCode}] -> ${redirectUrl}`);
        return downloadUrl(redirectUrl, dest, redirectCount + 1).then(resolve).catch(reject);
      }

      if (res.statusCode !== 200) {
        return reject(new Error(`HTTP ${res.statusCode} em ${url}`));
      }

      const fileStream = fs.createWriteStream(dest);
      res.pipe(fileStream);

      fileStream.on('finish', () => {
        fileStream.close();
        resolve(dest);
      });
      fileStream.on('error', (err) => {
        fs.unlink(dest, () => {});
        reject(err);
      });
    });

    req.on('error', (err) => reject(err));
    req.setTimeout(12000, () => {
      req.destroy();
      reject(new Error(`Timeout HTTP em ${url}`));
    });
  });
}

async function run() {
  console.log("=== DESCARREGANDO E VERIFICANDO MODELO TFLITE MOBILEFACENET AUTÊNTICO ===");
  for (const url of candidateUrls) {
    console.log(`\nTENTANDO: ${url}`);
    try {
      await downloadUrl(url, destPath);
      const stats = fs.statSync(destPath);
      if (stats.size > 200000) { // Must be a real neural weights model file (>200 KB)
        console.log(`\n[SUCESSO] Modelo autêntico baixado com sucesso!`);
        console.log(`Caminho: ${destPath}`);
        console.log(`Tamanho: ${stats.size} bytes (${(stats.size / (1024 * 1024)).toFixed(2)} MB)`);
        const fileBuf = fs.readFileSync(destPath);
        const hash = crypto.createHash('sha256').update(fileBuf).digest('hex').toUpperCase();
        console.log(`SHA-256: ${hash}`);
        return;
      } else {
        console.log(`[RECUSADO] Arquivo de apenas ${stats.size} bytes não contém matriz de pesos completa.`);
        fs.unlinkSync(destPath);
      }
    } catch (err) {
      console.log(`[FALHA] ${err.message}`);
    }
  }
  console.log("\n[STATUS]: Nenhuma das URLs retornou o arquivo `.tflite` com os pesos remotos.");
}

run();
