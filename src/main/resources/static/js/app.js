'use strict';

/* ---------------------------------------------------------------------------
 * Orcamento IA - interface web
 *
 * O ditado de voz roda no proprio navegador (Web Speech API) e envia texto para
 * /api/assistente/texto. Isso mantem o fluxo de voz funcionando com o modelo
 * local, que nao transcreve audio. Quando ha chave da OpenAI, o audio bruto vai
 * para /api/assistente/audio e quem transcreve e o Whisper, no servidor.
 * ------------------------------------------------------------------------- */

const MOEDA = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const DATA = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit' });

const estado = {
    transacoes: [],
    categorias: { despesas: [], receitas: [] },
    salario: null,
    tipoLancamento: 'DESPESA',
    filtro: 'TODOS',
    iaConfigurada: false,
    transcricaoServidor: false,
    ocupado: false,
    gravando: false
};

const el = (id) => document.getElementById(id);

const $microfone = el('botao-microfone');
const $gravadorEstado = el('gravador-estado');
const $gravadorDica = el('gravador-dica');
const $conversa = el('conversa');
const $entradaTexto = el('entrada-texto');
const $notificacao = el('notificacao');

/* ---------------------------------------------------------------- utilidades */

function notificar(texto, tipo = 'ok') {
    $notificacao.textContent = texto;
    $notificacao.className = `notificacao notificacao--${tipo} visivel`;
    $notificacao.hidden = false;
    clearTimeout(notificar._t);
    notificar._t = setTimeout(() => {
        $notificacao.classList.remove('visivel');
        setTimeout(() => { $notificacao.hidden = true; }, 200);
    }, 3200);
}

// Os erros da API saem em ProblemDetail (RFC 7807); o campo util e o "detail".
async function lerErro(resposta) {
    try {
        const corpo = await resposta.json();
        return corpo.detail || corpo.title || `Erro ${resposta.status}`;
    } catch {
        return `Erro ${resposta.status}`;
    }
}

// Botao de lixeira das tabelas. A confirmacao evita apagar por engano num
// clique so, ja que nao existe desfazer.
function criarBotaoApagar(rotulo, aoConfirmar) {
    const botao = document.createElement('button');
    botao.type = 'button';
    botao.className = 'apagar';
    botao.title = `Apagar ${rotulo}`;
    botao.setAttribute('aria-label', `Apagar ${rotulo}`);
    botao.innerHTML = '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"'
        + ' stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18M8 6V4a1 1 0 0 1 1-1h6a1 1 0'
        + ' 0 1 1 1v2m2 0v14a1 1 0 0 1-1 1H7a1 1 0 0 1-1-1V6M10 11v6M14 11v6"/></svg>';
    botao.addEventListener('click', async () => {
        if (!confirm(`Apagar ${rotulo}?`)) return;
        botao.disabled = true;
        try {
            await aoConfirmar();
        } catch (e) {
            notificar(e.message || 'Não foi possível apagar.', 'erro');
            botao.disabled = false;
        }
    });
    return botao;
}

async function apagar(url, mensagem) {
    const resposta = await fetch(url, { method: 'DELETE' });
    if (!resposta.ok) throw new Error(await lerErro(resposta));
    notificar(mensagem, 'ok');
}

/* ------------------------------------------------------------------ conversa */

function limparConversaVazia() {
    const vazia = $conversa.querySelector('.conversa-vazia');
    if (vazia) vazia.remove();
}

function adicionarMensagem(autor, texto, variante = '') {
    limparConversaVazia();
    const div = document.createElement('div');
    div.className = `mensagem mensagem--${variante || autor}`;
    const rotulo = document.createElement('span');
    rotulo.className = 'mensagem-autor';
    rotulo.textContent = autor === 'usuario' ? 'Você' : 'Assistente';
    const corpo = document.createElement('div');
    corpo.className = 'mensagem-texto';
    corpo.textContent = texto;
    div.append(rotulo, corpo);
    $conversa.appendChild(div);
    $conversa.scrollTop = $conversa.scrollHeight;
    return div;
}

function adicionarCarregando() {
    limparConversaVazia();
    const div = document.createElement('div');
    div.className = 'mensagem mensagem--assistente';
    div.innerHTML = '<span class="mensagem-autor">Assistente</span>'
        + '<div class="mensagem-texto"><span class="digitando"><span></span><span></span><span></span></span></div>';
    $conversa.appendChild(div);
    $conversa.scrollTop = $conversa.scrollHeight;
    return div;
}

/* -------------------------------------------------------------------- estado */

function definirOcupado(ocupado) {
    estado.ocupado = ocupado;

    // O comando ESCRITO nunca depende de chave: sem ela, o interpretador
    // proprio atende. Bloquear o campo aqui esconderia um recurso que funciona.
    $entradaTexto.disabled = ocupado;
    document.querySelectorAll('#sugestoes button, #form-texto button')
        .forEach((b) => { b.disabled = ocupado; });

    // A VOZ, sim, depende: a transcricao e feita pela OpenAI.
    $microfone.disabled = ocupado;
    $microfone.classList.toggle('ocupado', ocupado);
    $microfone.classList.toggle('sem-chave', !estado.iaConfigurada);
}

/* -------------------------------------------------------------- dados / API */

async function carregarStatus() {
    try {
        const resposta = await fetch('/api/assistente/status');
        const status = await resposta.json();
        estado.iaConfigurada = status.iaConfigurada;
        estado.transcricaoServidor = status.transcricaoServidor;

        const pilula = el('pilula-status');
        const texto = el('texto-status');
        // Esconder aqui e obrigatorio: o aviso e mostrado no ramo de baixo e,
        // sem isto, continuava na tela depois de a chave ser configurada.
        el('aviso').hidden = status.iaConfigurada;

        // O cartao do microfone precisa dizer o que acontece ao tocar nele.
        const dica = el('dica-microfone');
        if (dica) {
            dica.textContent = status.iaConfigurada
                ? 'Ex.: “Gastei 50 reais com almoço na categoria alimentação”'
                : 'A voz precisa da chave da OpenAI. Toque para configurar.';
        }

        if (status.iaConfigurada) {
            pilula.className = 'pilula pilula--ok';
            // O rotulo segue o provedor real informado pelo backend.
            texto.textContent = status.provedor === 'openai' ? 'OpenAI conectada' : 'Modelo local ativo';
        } else {
            pilula.className = 'pilula pilula--alerta';
            texto.textContent = 'IA não configurada';
            el('aviso-texto').textContent = '';
            el('aviso').hidden = false;
        }
        definirOcupado(false);
    } catch {
        el('texto-status').textContent = 'Indisponível';
    }
}

// A autoria vem do servidor (/api/sobre), nao escrita no HTML: assim o nome
// tem uma fonte unica, no backend, em vez de ser marcacao solta.
async function carregarSobre() {
    const resposta = await fetch('/api/sobre');
    const sobre = await resposta.json();

    el('autor-topo').textContent = sobre.autor;
    el('autor-nome').textContent = sobre.autor;
    el('autor-descricao').textContent = sobre.descricao;
    el('autor-inicial').textContent = sobre.autor
        .split(' ')
        .map((parte) => parte[0])
        .join('')
        .slice(0, 2)
        .toUpperCase();
    // Link so aparece se houver endereco: perfil em branco fica escondido em
    // vez de virar um link quebrado.
    [['autor-github', sobre.github], ['autor-linkedin', sobre.linkedin],
     ['autor-instagram', sobre.instagram], ['autor-site', sobre.site]]
        .forEach(([id, url]) => {
            const link = el(id);
            if (url) { link.href = url; link.hidden = false; } else { link.hidden = true; }
        });
    document.title = `${sobre.projeto} — ${sobre.autor}`;
}

async function carregarTransacoes() {
    const [listaResp, saldoResp] = await Promise.all([
        fetch('/api/transacoes'),
        fetch('/api/transacoes/saldo')
    ]);
    estado.transacoes = await listaResp.json();
    const saldo = await saldoResp.json();
    renderizarResumo(Number(saldo));
    renderizarTabela();
}

function renderizarResumo(saldo) {
    const soma = (tipo) => estado.transacoes
        .filter((t) => t.tipo === tipo)
        .reduce((total, t) => total + Number(t.valor), 0);

    const receitas = soma('RECEITA');
    const despesas = soma('DESPESA');
    const qtd = (tipo) => estado.transacoes.filter((t) => t.tipo === tipo).length;

    const $saldo = el('valor-saldo');
    $saldo.textContent = MOEDA.format(saldo);
    $saldo.classList.toggle('negativo', saldo < 0);
    el('legenda-saldo').textContent = saldo < 0 ? 'Orçamento no vermelho' : 'Receitas menos despesas';

    el('valor-receitas').textContent = MOEDA.format(receitas);
    el('valor-despesas').textContent = MOEDA.format(despesas);

    const plural = (n) => `${n} ${n === 1 ? 'lançamento' : 'lançamentos'}`;
    el('legenda-receitas').textContent = qtd('RECEITA') ? plural(qtd('RECEITA')) : 'Nenhum lançamento';
    el('legenda-despesas').textContent = qtd('DESPESA') ? plural(qtd('DESPESA')) : 'Nenhum lançamento';
}

function renderizarTabela(idDestaque = null) {
    const corpo = el('corpo-tabela');
    const lista = estado.filtro === 'TODOS'
        ? estado.transacoes
        : estado.transacoes.filter((t) => t.tipo === estado.filtro);

    corpo.innerHTML = '';
    el('tabela-vazia').hidden = lista.length > 0;

    // Mais recentes primeiro.
    [...lista].sort((a, b) => b.id - a.id).forEach((t) => {
        const receita = t.tipo === 'RECEITA';
        const tr = document.createElement('tr');
        if (t.id === idDestaque) tr.className = 'linha-nova';

        const tdDesc = document.createElement('td');
        const wrap = document.createElement('div');
        wrap.className = 'celula-descricao';
        const marca = document.createElement('span');
        marca.className = `indicador indicador--${receita ? 'receita' : 'despesa'}`;
        marca.textContent = receita ? '↑' : '↓';
        marca.setAttribute('aria-label', receita ? 'Receita' : 'Despesa');
        const nome = document.createElement('span');
        nome.textContent = t.descricao;
        wrap.append(marca, nome);
        tdDesc.appendChild(wrap);

        const tdCat = document.createElement('td');
        const cat = document.createElement('span');
        cat.className = 'marca-categoria';
        cat.textContent = rotuloCategoria(t.categoria);
        tdCat.appendChild(cat);

        const tdData = document.createElement('td');
        tdData.className = 'celula-data';
        tdData.textContent = DATA.format(new Date(t.dataHora));

        const tdValor = document.createElement('td');
        tdValor.className = `direita celula-valor celula-valor--${receita ? 'receita' : 'despesa'}`;
        tdValor.textContent = (receita ? '+' : '−') + ' ' + MOEDA.format(Number(t.valor));

        const tdAcao = document.createElement('td');
        tdAcao.className = 'direita celula-acao';
        tdAcao.appendChild(criarBotaoApagar(`"${t.descricao}"`, async () => {
            await apagar(`/api/transacoes/${t.id}`, 'Transação apagada.');
            await carregarTransacoes();
        }));

        tr.append(tdDesc, tdCat, tdData, tdValor, tdAcao);
        corpo.appendChild(tr);
    });
}

// As categorias vem do backend (enums CategoriaDespesa/CategoriaReceita) para
// que a interface nao mantenha uma copia propria da lista.
async function carregarCategorias() {
    const resposta = await fetch('/api/categorias');
    estado.categorias = await resposta.json();
    preencherCategorias(estado.tipoLancamento);
}

function preencherCategorias(tipo, selecionada = null) {
    const lista = tipo === 'RECEITA' ? estado.categorias.receitas : estado.categorias.despesas;
    const campo = el('campo-categoria');
    campo.innerHTML = lista
        .map((c) => `<option value="${c.valor}">${c.rotulo}</option>`)
        .join('');
    if (selecionada) campo.value = selecionada;
}

// A transacao guarda o valor cru ("alimentacao"); a IA tambem pode gravar uma
// categoria que nao esta na lista, entao caimos no proprio valor nesse caso.
function rotuloCategoria(valor) {
    const todas = [...estado.categorias.despesas, ...estado.categorias.receitas];
    const achada = todas.find((c) => c.valor === valor);
    return achada ? achada.rotulo : valor;
}

async function carregarConfiguracao() {
    const resposta = await fetch('/api/configuracao');
    const config = await resposta.json();
    estado.salario = config.salario;
    if (config.salario != null) el('campo-salario').value = config.salario;
    if (config.diaRecebimento != null) el('campo-dia').value = config.diaRecebimento;
    // A chave nunca volta do servidor: so o fato de existir uma configurada.
    const temChave = Boolean(config.chaveOpenAiConfigurada);
    el('estado-chave').hidden = !temChave;
    el('remover-chave').hidden = !temChave;
    el('campo-chave-openai').placeholder = temChave ? '••••••••  (guardada)' : 'sk-...';

    // Primeiro acesso: pede o salario antes de liberar a tela.
    el('boas-vindas').hidden = config.configurado;
    if (!config.configurado) setTimeout(() => el('salario-inicial').focus(), 60);
}

async function salvarSalario(valor, dia = null) {
    const resposta = await fetch('/api/configuracao/salario', {
        method: 'PUT',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            salario: Number(valor),
            diaRecebimento: dia ? Number(dia) : null
        })
    });
    if (!resposta.ok) throw new Error(await lerErro(resposta));
    const config = await resposta.json();
    estado.salario = config.salario;
    el('campo-salario').value = config.salario;
    return config;
}

/* -------------------------------------------------------------- assistente */

async function enviarComando(comando) {
    if (!comando.trim() || estado.ocupado) return;

    // Sem trava por falta de chave: quem decide como responder e o servidor.
    // Com chave usa a OpenAI; sem chave, o interpretador proprio atende. Uma
    // verificacao aqui impediria o comando de sair e esconderia o interpretador
    // inteiro — foi exatamente o que aconteceu.

    adicionarMensagem('usuario', comando);
    definirOcupado(true);
    const carregando = adicionarCarregando();

    try {
        const resposta = await fetch('/api/assistente/texto', {
            method: 'POST',
            headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
            body: new URLSearchParams({ comando })
        });
        carregando.remove();

        if (!resposta.ok) {
            adicionarMensagem('assistente', await lerErro(resposta), 'erro');
            return;
        }
        const dados = await resposta.json();
        adicionarMensagem('assistente', dados.respostaAssistente);
        // O assistente pode ter registrado algo via Tool Calling.
        await carregarTransacoes();
    } catch (e) {
        carregando.remove();
        adicionarMensagem('assistente', 'Não foi possível falar com o servidor.', 'erro');
    } finally {
        definirOcupado(false);
    }
}

async function enviarAudio(blob, extensao) {
    definirOcupado(true);
    const carregando = adicionarCarregando();
    try {
        const dadosForm = new FormData();
        // O servico de transcricao usa o nome do arquivo para inferir o formato,
        // entao a extensao precisa ir junto.
        dadosForm.append('arquivo', blob, `comando.${extensao}`);

        const resposta = await fetch('/api/assistente/audio', { method: 'POST', body: dadosForm });
        carregando.remove();

        if (!resposta.ok) {
            adicionarMensagem('assistente', await lerErro(resposta), 'erro');
            return;
        }
        const dados = await resposta.json();
        adicionarMensagem('usuario', dados.textoTranscrito);
        adicionarMensagem('assistente', dados.respostaAssistente);
        await carregarTransacoes();
    } catch {
        carregando.remove();
        adicionarMensagem('assistente', 'Não foi possível enviar o áudio.', 'erro');
    } finally {
        definirOcupado(false);
    }
}

/* ------------------------------------------------------------------ ditado */

const Reconhecimento = window.SpeechRecognition || window.webkitSpeechRecognition;

function iniciarDitadoNavegador() {
    const rec = new Reconhecimento();
    rec.lang = 'pt-BR';
    rec.interimResults = true;
    rec.continuous = false;
    let transcrito = '';

    rec.onstart = () => {
        estado.gravando = true;
        $microfone.classList.add('gravando');
        $gravadorEstado.textContent = 'Ouvindo…';
        $gravadorDica.textContent = 'Fale seu comando e toque de novo para enviar.';
    };
    rec.onresult = (evento) => {
        transcrito = Array.from(evento.results).map((r) => r[0].transcript).join('');
        $gravadorDica.textContent = transcrito || 'Fale seu comando…';
    };
    rec.onerror = (evento) => {
        pararVisualDitado();
        const motivo = evento.error === 'not-allowed'
            ? 'Permissão de microfone negada.'
            : evento.error === 'no-speech'
                ? 'Não ouvi nada. Tente de novo.'
                : `Falha no reconhecimento de voz (${evento.error}).`;
        notificar(motivo, 'erro');
    };
    rec.onend = () => {
        pararVisualDitado();
        if (transcrito.trim()) enviarComando(transcrito.trim());
    };

    rec.start();
    return rec;
}

function pararVisualDitado() {
    estado.gravando = false;
    $microfone.classList.remove('gravando');
    $gravadorEstado.textContent = 'Toque para falar';
    $gravadorDica.textContent = 'Ex.: “Gastei 50 reais com almoço na categoria alimentação”';
}

/* ------------------------------------------- gravacao para o Whisper (opcional) */

let gravador = null;
let pedacos = [];
let cronometro = null;

const FORMATOS = [
    ['audio/webm', 'webm'],
    ['audio/mp4', 'mp4'],
    ['audio/ogg', 'ogg']
];

async function iniciarGravacao() {
    const fluxo = await navigator.mediaDevices.getUserMedia({ audio: true });
    const suportado = FORMATOS.find(([mime]) => MediaRecorder.isTypeSupported(mime));
    const [mime, extensao] = suportado || [undefined, 'webm'];

    gravador = new MediaRecorder(fluxo, mime ? { mimeType: mime } : undefined);
    pedacos = [];

    gravador.ondataavailable = (e) => { if (e.data.size > 0) pedacos.push(e.data); };
    gravador.onstop = () => {
        fluxo.getTracks().forEach((t) => t.stop());
        clearInterval(cronometro);
        pararVisualDitado();
        if (pedacos.length) enviarAudio(new Blob(pedacos, { type: mime || 'audio/webm' }), extensao);
    };

    gravador.start();
    estado.gravando = true;
    $microfone.classList.add('gravando');

    const inicio = Date.now();
    const tick = () => {
        const s = Math.floor((Date.now() - inicio) / 1000);
        $gravadorEstado.textContent = `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}`;
    };
    tick();
    $gravadorDica.textContent = 'Gravando… toque novamente para enviar.';
    cronometro = setInterval(tick, 500);
}

/* ------------------------------------------------------------------ eventos */

let reconhecimentoAtivo = null;

$microfone.addEventListener('click', async () => {
    if (estado.ocupado) return;

    if (estado.gravando) {
        if (reconhecimentoAtivo) { reconhecimentoAtivo.stop(); reconhecimentoAtivo = null; }
        else if (gravador && gravador.state === 'recording') gravador.stop();
        return;
    }

    // Sem chave da OpenAI o assistente nao responde, e o microfone nao teria
    // para onde mandar o que fosse ditado. Dizer "configure a chave" e apontar
    // ONDE evita o clique que nao faz nada.
    if (!estado.iaConfigurada) {
        notificar('O assistente precisa da sua chave da OpenAI. Configure em Configurações › Voz.', 'erro');
        el('modal-config').hidden = false;
        setTimeout(() => el('campo-chave-openai').focus(), 80);
        return;
    }

    try {
        // Preferimos o ditado do navegador: nao gasta credito de transcricao.
        if (Reconhecimento) {
            reconhecimentoAtivo = iniciarDitadoNavegador();
        } else if (estado.transcricaoServidor) {
            await iniciarGravacao();
        } else {
            notificar('Seu navegador não faz ditado. Use o Chrome, ou digite o comando aqui.', 'erro');
        }
    } catch (e) {
        pararVisualDitado();
        notificar('Não foi possível acessar o microfone.', 'erro');
    }
});

el('form-texto').addEventListener('submit', (e) => {
    e.preventDefault();
    const comando = $entradaTexto.value;
    $entradaTexto.value = '';
    enviarComando(comando);
});

el('sugestoes').addEventListener('click', (e) => {
    const botao = e.target.closest('button');
    if (botao) enviarComando(botao.dataset.comando);
});

el('filtros').addEventListener('click', (e) => {
    const botao = e.target.closest('button');
    if (!botao) return;
    estado.filtro = botao.dataset.filtro;
    el('filtros').querySelectorAll('button').forEach((b) => b.classList.toggle('ativo', b === botao));
    renderizarTabela();
});

el('form-transacao').addEventListener('submit', async (e) => {
    e.preventDefault();
    const form = e.target;
    const erro = el('erro-formulario');
    erro.hidden = true;

    const dados = Object.fromEntries(new FormData(form));
    const botao = form.querySelector('button[type=submit]');
    botao.disabled = true;

    try {
        const resposta = await fetch('/api/transacoes', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                descricao: dados.descricao,
                valor: Number(dados.valor),
                categoria: dados.categoria,
                tipo: estado.tipoLancamento
            })
        });

        if (!resposta.ok) {
            erro.textContent = await lerErro(resposta);
            erro.hidden = false;
            return;
        }
        const criada = await resposta.json();
        form.reset();
        await carregarTransacoes();
        renderizarTabela(criada.id);
        notificar('Lançamento registrado.', 'ok');
    } catch {
        erro.textContent = 'Não foi possível falar com o servidor.';
        erro.hidden = false;
    } finally {
        botao.disabled = false;
    }
});

/* ------------------------------------------ configuracao e boas-vindas */

// Trocar o tipo troca a lista de categorias: despesa e receita tem opcoes
// diferentes (alimentacao/transporte... contra salario/presente/extra).
// Um formulario so para gasto e entrada: trocar o tipo troca a lista de
// categorias, o rotulo do primeiro campo e a cor do botao.
function selecionarTipo(tipo) {
    estado.tipoLancamento = tipo;
    const receita = tipo === 'RECEITA';

    el('seletor-tipo').querySelectorAll('button').forEach((b) => {
        b.classList.toggle('ativo', b.dataset.tipo === tipo);
    });
    el('rotulo-descricao').textContent = receita ? 'De onde veio' : 'Com o quê';
    el('form-transacao').querySelector('input[name=descricao]').placeholder =
        receita ? 'Presente da minha avó' : 'Almoço no restaurante';
    el('botao-lancar').textContent = receita ? 'Adicionar à conta' : 'Registrar gasto';
    el('botao-lancar').classList.toggle('botao--verde', receita);
    preencherCategorias(tipo);
}

el('seletor-tipo').addEventListener('click', (e) => {
    const botao = e.target.closest('button');
    if (botao) selecionarTipo(botao.dataset.tipo);
});

el('form-boas-vindas').addEventListener('submit', async (e) => {
    e.preventDefault();
    const erro = el('erro-boas-vindas');
    const botao = e.target.querySelector('button[type=submit]');
    erro.hidden = true;
    botao.disabled = true;
    try {
        await salvarSalario(el('salario-inicial').value);
        el('boas-vindas').hidden = true;
        await carregarTransacoes();
        notificar('Orçamento configurado. Bom controle!', 'ok');
    } catch (falha) {
        erro.textContent = falha.message;
        erro.hidden = false;
    } finally {
        botao.disabled = false;
    }
});

// Saida para quem vive de renda variavel: sai das boas-vindas sem informar
// valor nenhum. Inventar um salario aqui entraria como receita no saldo e
// mentiria sobre quanto a pessoa tem.
el('sem-salario').addEventListener('click', async () => {
    const erro = el('erro-boas-vindas');
    const botao = el('sem-salario');
    erro.hidden = true;
    botao.disabled = true;
    try {
        const resposta = await fetch('/api/configuracao/sem-salario', { method: 'PUT' });
        if (!resposta.ok) throw new Error('Nao foi possivel salvar. Tente de novo.');
        const config = await resposta.json();
        estado.salario = config.salario;
        el('boas-vindas').hidden = true;
        await carregarTransacoes();
        notificar('Pronto. Registre cada entrada de dinheiro conforme receber.', 'ok');
    } catch (falha) {
        erro.textContent = falha.message;
        erro.hidden = false;
    } finally {
        botao.disabled = false;
    }
});

el('botao-config').addEventListener('click', () => {
    el('modal-config').hidden = false;
    setTimeout(() => el('campo-salario').focus(), 60);
});
el('fechar-config').addEventListener('click', () => { el('modal-config').hidden = true; });
el('modal-config').addEventListener('click', (e) => {
    // Clicar no fundo escuro fecha; clicar dentro da caixa nao.
    if (e.target === el('modal-config')) el('modal-config').hidden = true;
});
document.addEventListener('keydown', (e) => {
    if (e.key === 'Escape' && !el('modal-config').hidden) el('modal-config').hidden = true;
});

el('form-salario').addEventListener('submit', async (e) => {
    e.preventDefault();
    const erro = el('erro-salario');
    const botao = e.target.querySelector('button[type=submit]');
    erro.hidden = true;
    botao.disabled = true;
    try {
        await salvarSalario(el('campo-salario').value, el('campo-dia').value);
        await carregarTransacoes();
        el('modal-config').hidden = true;
        notificar('Salário atualizado.', 'ok');
    } catch (falha) {
        erro.textContent = falha.message;
        erro.hidden = false;
    } finally {
        botao.disabled = false;
    }
});

/* ------------------------------------------------------- investimentos */

// O porquinho tem total proprio e nao entra no saldo do orcamento.
async function carregarInvestimentos(idDestaque = null) {
    const [listaResp, resumoResp] = await Promise.all([
        fetch('/api/investimentos'),
        fetch('/api/investimentos/resumo')
    ]);
    const lista = await listaResp.json();
    const resumo = await resumoResp.json();

    // O numero grande e o saldo ja corrigido pelo CDI, como num extrato.
    el('valor-porquinho').textContent = MOEDA.format(Number(resumo.totalComRendimento));
    el('valor-depositado').textContent = MOEDA.format(Number(resumo.total));
    el('valor-rendimento').textContent = '+ ' + MOEDA.format(Number(resumo.rendimento));
    el('taxa-cdi').textContent = `100% do CDI (${(Number(resumo.cdiAnual) * 100).toFixed(2).replace('.', ',')}% a.a.)`;
    el('legenda-porquinho').textContent = lista.length
        ? `${MOEDA.format(Number(resumo.aportes))} guardados · ${MOEDA.format(Number(resumo.retiradas))} retirados`
        : 'Separado do saldo do orçamento';

    const corpo = el('corpo-investimentos');
    corpo.innerHTML = '';
    el('investimentos-vazio').hidden = lista.length > 0;

    [...lista].sort((a, b) => b.id - a.id).forEach((i) => {
        const aporte = i.tipo === 'APORTE';
        const tr = document.createElement('tr');
        if (i.id === idDestaque) tr.className = 'linha-nova';

        const tdDesc = document.createElement('td');
        const wrap = document.createElement('div');
        wrap.className = 'celula-descricao';
        const marca = document.createElement('span');
        marca.className = `indicador indicador--${aporte ? 'aporte' : 'retirada'}`;
        marca.textContent = aporte ? '↑' : '↓';
        marca.setAttribute('aria-label', aporte ? 'Guardado' : 'Retirado');
        const nome = document.createElement('span');
        nome.textContent = i.descricao;
        wrap.append(marca, nome);
        tdDesc.appendChild(wrap);

        const tdTipo = document.createElement('td');
        const chip = document.createElement('span');
        chip.className = 'marca-categoria';
        chip.textContent = aporte ? 'Guardado' : 'Retirado';
        tdTipo.appendChild(chip);

        const tdData = document.createElement('td');
        tdData.className = 'celula-data';
        tdData.textContent = DATA.format(new Date(i.dataHora));

        const tdValor = document.createElement('td');
        tdValor.className = `direita celula-valor celula-valor--${aporte ? 'aporte' : 'retirada'}`;
        tdValor.textContent = (aporte ? '+' : '−') + ' ' + MOEDA.format(Number(i.valor));

        const tdAcao = document.createElement('td');
        tdAcao.className = 'direita celula-acao';
        tdAcao.appendChild(criarBotaoApagar(`"${i.descricao}"`, async () => {
            await apagar(`/api/investimentos/${i.id}`, 'Movimento apagado.');
            await carregarInvestimentos();
        }));

        tr.append(tdDesc, tdTipo, tdData, tdValor, tdAcao);
        corpo.appendChild(tr);
    });
}

el('form-investimento').addEventListener('submit', async (e) => {
    e.preventDefault();
    const form = e.target;
    const erro = el('erro-investimento');
    const botao = form.querySelector('button[type=submit]');
    const dados = Object.fromEntries(new FormData(form));
    erro.hidden = true;
    botao.disabled = true;

    try {
        const resposta = await fetch('/api/investimentos', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                descricao: dados.descricao,
                valor: Number(dados.valor),
                tipo: dados.tipo
            })
        });
        if (!resposta.ok) {
            erro.textContent = await lerErro(resposta);
            erro.hidden = false;
            return;
        }
        const criado = await resposta.json();
        form.reset();
        await carregarInvestimentos(criado.id);
        notificar(dados.tipo === 'APORTE' ? 'Valor guardado no porquinho.' : 'Retirada registrada.', 'ok');
    } catch {
        erro.textContent = 'Não foi possível falar com o servidor.';
        erro.hidden = false;
    } finally {
        botao.disabled = false;
    }
});

el('abas').addEventListener('click', (e) => {
    const botao = e.target.closest('button');
    if (!botao) return;
    const aba = botao.dataset.aba;
    el('abas').querySelectorAll('button').forEach((b) => {
        const ativa = b === botao;
        b.classList.toggle('ativa', ativa);
        b.setAttribute('aria-selected', String(ativa));
    });
    el('painel-transacoes').hidden = aba !== 'transacoes';
    el('painel-investimentos').hidden = aba !== 'investimentos';
    el('painel-fixos').hidden = aba !== 'fixos';
    el('painel-resumo').hidden = aba !== 'resumo';

    // Recarrega ao abrir a aba, e nao no carregamento da pagina: e assim que a
    // tabela fica sempre atual sem precisar de botao de atualizar.
    if (aba === 'fixos') { preencherCategoriasDoFixo(); carregarFixos(); carregarFecharMes(); }
    if (aba === 'resumo') carregarResumo();
});

/* --------------------------------------------------- fixos e resumo ---- */

let periodoDoResumo = 'mes';

// Escapa antes de interpolar em HTML: a descricao vem do usuario e pode conter
// < ou &, que quebrariam a tabela — ou pior, injetariam marcacao.
function texto(valor) {
    return String(valor ?? '').replace(/[&<>"']/g, (c) => ({
        '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
    })[c]);
}

// O select de categoria dos fixos usa a mesma fonte unica do backend.
function preencherCategoriasDoFixo() {
    const tipo = el('fixo-tipo').value;
    const lista = tipo === 'RECEITA' ? estado.categorias.receitas : estado.categorias.despesas;
    el('fixo-categoria').innerHTML = lista
        .map((c) => `<option value="${c.valor}">${c.rotulo}</option>`)
        .join('');
}

el('fixo-tipo').addEventListener('change', preencherCategoriasDoFixo);

async function carregarFixos() {
    const area = el('lista-fixos');
    try {
        const itens = await (await fetch('/api/fixos')).json();
        if (!itens.length) {
            area.innerHTML = '<p class="vazio">Nenhum item fixo ainda. Cadastre acima.</p>';
            return;
        }
        const linha = (i) => `
            <tr>
                <td>${texto(i.descricao)}</td>
                <td><span class="etiqueta">${texto(rotuloCategoria(i.categoria))}</span></td>
                <td>${i.tipo === 'RECEITA' ? 'Ganho' : 'Gasto'}</td>
                <td>${i.valorPrevisto == null
                        ? '<em class="variavel">varia</em>'
                        : MOEDA.format(i.valorPrevisto)}</td>
                <td>${i.diaVencimento == null ? '—' : 'dia ' + i.diaVencimento}</td>
                <td class="acoes">
                    <button type="button" class="mini" data-lancar="${i.id}"
                        data-desc="${texto(i.descricao)}" data-previsto="${i.valorPrevisto ?? ''}">Lançar</button>
                    <button type="button" class="mini mini--perigo" data-apagar-fixo="${i.id}">Apagar</button>
                </td>
            </tr>`;
        area.innerHTML = `<table class="tabela"><thead><tr>
            <th>O quê</th><th>Categoria</th><th>Tipo</th><th>Previsto</th><th>Dia</th><th></th>
            </tr></thead><tbody>${itens.map(linha).join('')}</tbody></table>`;
    } catch {
        area.innerHTML = '<p class="vazio">Não foi possível carregar os itens fixos.</p>';
    }
}

async function carregarResumo() {
    const area = el('tabela-resumo');
    try {
        const r = await (await fetch(`/api/resumo?mes=${periodoDoResumo === 'mes'}`)).json();
        if (!r.despesasPorCategoria.length && !r.receitasPorCategoria.length) {
            area.innerHTML = '<p class="vazio">Nenhum lançamento no período.</p>';
            return;
        }
        const linha = (l) => `
            <tr>
                <td><span class="etiqueta">${texto(rotuloCategoria(l.categoria))}</span></td>
                <td>${MOEDA.format(l.total)}</td>
                <td>
                    <div class="barra"><div class="barra-preenchida" style="width:${l.percentual}%"></div></div>
                    <span class="percentual">${Number(l.percentual).toFixed(1)}%</span>
                </td>
                <td>${l.lancamentos}</td>
            </tr>`;
        area.innerHTML = `
            <table class="tabela">
                <thead><tr><th>Modalidade</th><th>Total gasto</th><th>Do total</th><th>Lanç.</th></tr></thead>
                <tbody>${r.despesasPorCategoria.map(linha).join('')}</tbody>
            </table>
            <div class="resumo-totais">
                <div><span>Gastos</span><strong class="negativo">${MOEDA.format(r.totalDespesas)}</strong></div>
                <div><span>Receitas</span><strong class="positivo">${MOEDA.format(r.totalReceitas)}</strong></div>
                <div><span>Saldo</span><strong>${MOEDA.format(r.saldo)}</strong></div>
            </div>`;
    } catch {
        area.innerHTML = '<p class="vazio">Não foi possível carregar o resumo.</p>';
    }
}

el('filtro-resumo').addEventListener('click', (e) => {
    const b = e.target.closest('button');
    if (!b) return;
    periodoDoResumo = b.dataset.periodo;
    el('filtro-resumo').querySelectorAll('button')
        .forEach((x) => x.classList.toggle('ativa', x === b));
    carregarResumo();
});

// A tela de fechar o mes: uma linha por conta, um campo de valor em cada, e um
// botao so. Sem isso, fechar um mes com oito contas eram oito dialogos.
async function carregarFecharMes() {
    const area = el('fechar-mes');
    try {
        const itens = await (await fetch('/api/fixos/mes')).json();
        if (!itens.length) {
            area.innerHTML = '<p class="vazio">Cadastre itens fixos para fechar o mês por aqui.</p>';
            el('botao-fechar-mes').hidden = true;
            return;
        }
        const pendentes = itens.filter((i) => !i.jaLancado);
        el('botao-fechar-mes').hidden = pendentes.length === 0;

        const linha = (i) => `
            <tr class="${i.jaLancado ? 'lancada' : ''}">
                <td>
                    ${texto(i.descricao)}
                    ${i.diaVencimento ? `<span class="dia">dia ${i.diaVencimento}</span>` : ''}
                </td>
                <td><span class="etiqueta">${texto(rotuloCategoria(i.categoria))}</span></td>
                <td>${i.tipo === 'RECEITA' ? 'Ganho' : 'Gasto'}</td>
                <td>${i.jaLancado
                    ? `<span class="concluido">✓ lançado ${MOEDA.format(i.valorLancado)}</span>`
                    : `<input type="number" step="0.01" min="0.01" class="valor-mes"
                             data-item="${i.id}"
                             placeholder="${i.valorPrevisto == null ? 'quanto veio?' : Number(i.valorPrevisto).toFixed(2)}">`}
                </td>
            </tr>`;

        area.innerHTML = `<table class="tabela"><thead><tr>
            <th>Conta</th><th>Categoria</th><th>Tipo</th><th>Valor deste mês</th>
            </tr></thead><tbody>${itens.map(linha).join('')}</tbody></table>`;
    } catch {
        area.innerHTML = '<p class="vazio">Não foi possível carregar as contas do mês.</p>';
    }
}

el('botao-fechar-mes').addEventListener('click', async () => {
    const campos = [...document.querySelectorAll('.valor-mes')];
    const valores = {};
    let semValor = [];

    campos.forEach((c) => {
        const digitado = c.value.trim();
        if (digitado === '') {
            // Vazio significa "usar a previsao". Quem nao tem previsao precisa
            // de valor: lancar sem saber quanto foi encheria o saldo de chute.
            if (c.placeholder === 'quanto veio?') {
                semValor.push(c.closest('tr').firstElementChild.textContent.trim());
                return;
            }
            valores[c.dataset.item] = null;
        } else {
            valores[c.dataset.item] = Number(digitado.replace(',', '.'));
        }
    });

    if (!Object.keys(valores).length) {
        notificar(semValor.length
            ? 'Informe o valor das contas variáveis antes de fechar o mês.'
            : 'Nada pendente neste mês.', 'erro');
        return;
    }

    const botao = el('botao-fechar-mes');
    botao.disabled = true;
    try {
        const r = await fetch('/api/fixos/fechar-mes', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ valores })
        });
        if (!r.ok) throw new Error((await r.json()).detail || 'Não foi possível fechar o mês.');
        const lancados = await r.json();
        await Promise.all([carregarFecharMes(), carregarTransacoes(), carregarResumo()]);
        notificar(semValor.length
            ? `${lancados.length} conta(s) lançada(s). Faltou o valor de: ${semValor.join(', ')}.`
            : `${lancados.length} conta(s) lançada(s). Mês fechado.`, 'ok');
    } catch (falha) {
        notificar(falha.message, 'erro');
    } finally {
        botao.disabled = false;
    }
});

el('form-fixo').addEventListener('submit', async (e) => {
    e.preventDefault();
    const erro = el('erro-fixo');
    erro.hidden = true;
    const valor = el('fixo-valor').value;
    const dia = el('fixo-dia').value;
    try {
        const resposta = await fetch('/api/fixos', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                descricao: el('fixo-descricao').value,
                categoria: el('fixo-categoria').value,
                tipo: el('fixo-tipo').value,
                // Vazio vira null, e nao zero: e o que marca a conta como
                // variavel, para o usuario informar o valor ao lancar.
                valorPrevisto: valor === '' ? null : Number(valor),
                diaVencimento: dia === '' ? null : Number(dia)
            })
        });
        if (!resposta.ok) throw new Error((await resposta.json()).detail || 'Não foi possível cadastrar.');
        e.target.reset();
        await carregarFixos();
        notificar('Item fixo cadastrado.', 'ok');
    } catch (falha) {
        erro.textContent = falha.message;
        erro.hidden = false;
    }
});

el('lista-fixos').addEventListener('click', async (e) => {
    const lancar = e.target.closest('[data-lancar]');
    const apagar = e.target.closest('[data-apagar-fixo]');

    if (lancar) {
        const previsto = lancar.dataset.previsto;
        // Perguntar sempre, mesmo havendo previsao: a conta pode ter vindo
        // diferente, e e justamente esse o ponto da funcionalidade.
        const digitado = prompt(
            `Quanto veio "${lancar.dataset.desc}" neste mês?`,
            previsto || '');
        if (digitado === null) return;
        const valor = digitado.trim() === '' ? null : Number(digitado.replace(',', '.'));
        if (valor !== null && (!isFinite(valor) || valor <= 0)) {
            notificar('Informe um valor positivo.', 'erro');
            return;
        }
        try {
            const r = await fetch(`/api/fixos/${lancar.dataset.lancar}/lancar`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ valor })
            });
            if (!r.ok) throw new Error((await r.json()).detail || 'Não foi possível lançar.');
            await Promise.all([carregarTransacoes(), carregarResumo(), carregarFecharMes()]);
            notificar('Lançado no orçamento.', 'ok');
        } catch (falha) {
            notificar(falha.message, 'erro');
        }
        return;
    }

    if (apagar) {
        if (!confirm('Apagar este item fixo? Os lançamentos já feitos continuam no orçamento.')) return;
        await fetch(`/api/fixos/${apagar.dataset.apagarFixo}`, { method: 'DELETE' });
        await carregarFixos();
        notificar('Item fixo apagado.', 'ok');
    }
});

/* ------------------------------------------------------------------- inicio */

if (!Reconhecimento) {
    $gravadorDica.textContent = 'Ditado por voz disponível no Chrome. Você pode digitar o comando abaixo.';
}

// A ordem importa: categorias antes das transacoes, senao a tabela renderiza
// sem conseguir traduzir o valor da categoria para o rotulo exibido.
Promise.all([carregarStatus(), carregarCategorias(), carregarConfiguracao(), carregarSobre()])
    .then(() => Promise.all([carregarTransacoes(), carregarInvestimentos()]))
    .catch(() => notificar('Falha ao carregar os dados iniciais.', 'erro'));

/* ---------------------------------------------------------------- PWA ---- */

// Registra o service worker para o app poder ser instalado na tela inicial do
// celular. O registro falha em http:// fora de localhost — por isso o acesso
// pelo celular usa o IP da maquina na rede local, que os navegadores tratam
// como origem confiavel o suficiente para instalar.
if ('serviceWorker' in navigator) {
    window.addEventListener('load', () => {
        navigator.serviceWorker.register('/sw.js').catch(() => {
            /* Sem service worker o app continua funcionando, so nao instala. */
        });
    });
}

/* --------------------------------------------- chave da OpenAI ---- */

el('form-chave-openai').addEventListener('submit', async (e) => {
    e.preventDefault();
    const erro = el('erro-chave');
    const botao = e.target.querySelector('button[type=submit]');
    const chave = el('campo-chave-openai').value.trim();
    erro.hidden = true;
    if (!chave) {
        erro.textContent = 'Informe a chave.';
        erro.hidden = false;
        return;
    }
    botao.disabled = true;
    try {
        const resposta = await fetch('/api/configuracao/chave-openai', {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ chave })
        });
        if (!resposta.ok) throw new Error('Nao foi possivel salvar a chave.');
        el('campo-chave-openai').value = '';
        await carregarConfiguracao();
        await carregarStatus();
        notificar('Chave salva. A transcrição por voz já usa o servidor.', 'ok');
    } catch (falha) {
        erro.textContent = falha.message;
        erro.hidden = false;
    } finally {
        botao.disabled = false;
    }
});

el('remover-chave').addEventListener('click', async () => {
    const botao = el('remover-chave');
    botao.disabled = true;
    try {
        await fetch('/api/configuracao/chave-openai', { method: 'DELETE' });
        el('campo-chave-openai').value = '';
        await carregarConfiguracao();
        await carregarStatus();
        notificar('Chave removida. O ditado volta a ser feito pelo navegador.', 'ok');
    } finally {
        botao.disabled = false;
    }
});

/* ------------------------------------------------- Modo aplicativo ---- */

// Rodando como programa de duplo clique, fechar a janela precisa fechar o
// programa. A pagina avisa que esta viva enquanto estiver aberta; quando os
// avisos param, o servidor se encerra sozinho.
//
// So vale no modo aplicativo: no celular ou em servidor a aplicacao deve
// continuar de pe com a aba fechada, e mandar sinal seria trafego a toa.
const INTERVALO_SINAL = 4000;

function manterSessaoViva() {
    const sinal = () => {
        // keepalive permite que o ultimo sinal ainda saia enquanto a pagina se
        // fecha, evitando encerrar por engano quem so recarregou.
        fetch('/api/sessao/sinal', { method: 'POST', keepalive: true }).catch(() => {
            /* Servidor caiu ou esta reiniciando: nada a fazer aqui. */
        });
    };
    sinal();
    setInterval(sinal, INTERVALO_SINAL);

    // Fechar a janela avisa o servidor na hora. Sem isto, encerrar dependeria
    // do prazo do sinal de vida, que agora e longo de proposito: navegadores
    // congelam temporizadores de abas em segundo plano, e um prazo curto
    // matava a aplicacao so por o usuario trocar de aba.
    //
    // pagehide tambem dispara ao RECARREGAR; por isso o servidor espera alguns
    // segundos, e o primeiro sinal da pagina nova cancela o encerramento.
    const avisarFechamento = () => {
        const url = '/api/sessao/fechando';
        // sendBeacon sobrevive ao fechamento da pagina; fetch nem sempre.
        if (navigator.sendBeacon) navigator.sendBeacon(url);
        else fetch(url, { method: 'POST', keepalive: true }).catch(() => {});
    };
    window.addEventListener('pagehide', avisarFechamento);

    // Voltar para a aba retoma o sinal imediatamente, em vez de esperar o
    // proximo intervalo — o navegador pode ter congelado o temporizador.
    document.addEventListener('visibilitychange', () => {
        if (document.visibilityState === 'visible') sinal();
    });
}

fetch('/api/sessao')
    .then((r) => (r.ok ? r.json() : null))
    .then((sessao) => {
        if (!sessao) return;
        if (sessao.modoAplicativo) {
            manterSessaoViva();
        }

    })
    .catch(() => {
        /* Versao antiga do servidor ou offline: segue sem o encerramento. */
    });
