/*
 * Service worker do Fast Finance Helper.
 *
 * Ele existe por UM motivo: permitir instalar o aplicativo na tela inicial do
 * celular. Nada mais.
 *
 * Em especial, ele NAO guarda a interface em cache — e isso e deliberado.
 *
 * A aplicacao so funciona com o servidor rodando no computador: com o servidor
 * fora do ar, uma interface em cache abriria uma tela bonita que nao consegue
 * fazer nada. Em troca desse ganho inexistente, o cache trouxe problema real
 * duas vezes: uma mensagem de erro ja removida do codigo reapareceu na tela, e
 * abas novas nao apareciam mesmo com o servidor servindo o HTML novo. As duas
 * vezes o sintoma foi o mesmo: o codigo estava certo e a tela estava velha.
 *
 * Guardar so os icones e o manifest, que sao o que o navegador pede para
 * instalar, elimina a classe inteira de bug.
 */

const VERSAO = 'fast-finance-v3-sem-cache-de-interface';

const PARA_INSTALAR = [
    '/manifest.webmanifest',
    '/icones/icone-192.png',
    '/icones/icone-512.png'
];

self.addEventListener('install', (evento) => {
    evento.waitUntil(
        caches.open(VERSAO)
            .then((cache) => cache.addAll(PARA_INSTALAR))
            .then(() => self.skipWaiting())
    );
});

self.addEventListener('activate', (evento) => {
    // Apaga TUDO que ficou de versoes anteriores, inclusive a interface que
    // versoes antigas guardavam.
    evento.waitUntil(
        caches.keys()
            .then((chaves) => Promise.all(
                chaves.filter((c) => c !== VERSAO).map((c) => caches.delete(c))))
            .then(() => self.clients.claim())
    );
});

self.addEventListener('fetch', (evento) => {
    const url = new URL(evento.request.url);

    // Icones e manifest podem vir do cache: sao estaveis e o navegador os pede
    // durante a instalacao, as vezes com a aba fechada.
    const instalavel = PARA_INSTALAR.some((caminho) => url.pathname === caminho);
    if (evento.request.method === 'GET' && instalavel) {
        evento.respondWith(
            caches.match(evento.request).then((guardado) => guardado || fetch(evento.request))
        );
        return;
    }

    // Todo o resto — HTML, JS, CSS e a API — vai direto para o servidor, sempre.
    // Sem interceptar, o navegador cuida do restante.
});
