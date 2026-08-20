/*
 * Service worker do Orcamento IA.
 *
 * Guarda apenas o "casco" da interface (HTML, CSS, JS e icones) para o app
 * abrir instantaneamente e nao ficar em branco quando a rede oscila.
 *
 * As chamadas a /api NAO sao cacheadas de proposito: saldo e transacoes
 * precisam ser sempre os do servidor. Mostrar um saldo velho de cache seria
 * pior do que mostrar um erro de conexao.
 */

const VERSAO = 'orcamento-ia-v1';
const CASCO = [
    '/',
    '/css/estilo.css',
    '/js/app.js',
    '/manifest.webmanifest',
    '/icones/icone-192.png',
    '/icones/icone-512.png'
];

self.addEventListener('install', (evento) => {
    evento.waitUntil(
        caches.open(VERSAO)
            .then((cache) => cache.addAll(CASCO))
            .then(() => self.skipWaiting())
    );
});

self.addEventListener('activate', (evento) => {
    // Remove caches de versoes anteriores, senao o app fica preso num CSS antigo.
    evento.waitUntil(
        caches.keys()
            .then((chaves) => Promise.all(
                chaves.filter((c) => c !== VERSAO).map((c) => caches.delete(c))))
            .then(() => self.clients.claim())
    );
});

self.addEventListener('fetch', (evento) => {
    const url = new URL(evento.request.url);

    if (evento.request.method !== 'GET' || url.pathname.startsWith('/api')) {
        return;
    }

    // Rede primeiro, cache como reserva: assim uma alteracao no CSS ou no JS
    // aparece na proxima visita, sem esperar o cache expirar.
    evento.respondWith(
        fetch(evento.request)
            .then((resposta) => {
                const copia = resposta.clone();
                caches.open(VERSAO).then((cache) => cache.put(evento.request, copia));
                return resposta;
            })
            .catch(() => caches.match(evento.request).then((r) => r || caches.match('/')))
    );
});
