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
    $microfone.disabled = ocupado || !estado.iaConfigurada;
    $entradaTexto.disabled = ocupado || !estado.iaConfigurada;
    document.querySelectorAll('#sugestoes button, #form-texto button')
        .forEach((b) => { b.disabled = ocupado || !estado.iaConfigurada; });
    $microfone.classList.toggle('ocupado', ocupado);
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
        if (status.iaConfigurada) {
            pilula.className = 'pilula pilula--ok';
            // O rotulo segue o provedor real informado pelo backend.
            texto.textContent = status.provedor === 'openai' ? 'OpenAI conectada' : 'Modelo local ativo';
        } else {
            pilula.className = 'pilula pilula--alerta';
            texto.textContent = 'IA não configurada';
            el('aviso-texto').textContent = ' ' + status.mensagem + ' ';
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
    if (!estado.iaConfigurada) {
        notificar('Configure a ANTHROPIC_API_KEY para usar o assistente.', 'erro');
        return;
    }

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

    try {
        // Preferimos o ditado do navegador: nao gasta credito de transcricao e
        // funciona so com a chave da Anthropic.
        if (Reconhecimento) {
            reconhecimentoAtivo = iniciarDitadoNavegador();
        } else if (estado.transcricaoServidor) {
            await iniciarGravacao();
        } else {
            notificar('Seu navegador não suporta ditado. Use o Chrome ou digite o comando.', 'erro');
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
