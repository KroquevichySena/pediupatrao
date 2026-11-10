(() => {
    'use strict';
    const $ = id => document.getElementById(id);
    const money = n => new Intl.NumberFormat('pt-BR', {style: 'currency', currency: 'BRL'}).format(n);
    const integer = n => new Intl.NumberFormat('pt-BR').format(n);
    const colors = ['#cca459', '#e2b251', '#4a9d83', '#6696bb', '#a092b3', '#18694f', '#d27b70', '#aaa'];
    let salesChart, statusChart, page = 0, busy = false, applied, descriptions = {};
    function text(tag, value, cls) { const el = document.createElement(tag); el.textContent = value; if (cls) el.className = cls; return el; }
    function day(value) { const [y,m,d] = value.split('-'); return d ? `${d}/${m}` : `${m}/${y}`; }
    async function json(url) {
        const response = await fetch(url, {headers: {'Accept': 'application/json'}, credentials: 'same-origin', cache: 'no-store', signal: AbortSignal.timeout(20000)});
        if (response.status === 401) throw new Error('Sua sessão expirou. Entre novamente para consultar o painel.');
        if (response.status === 403) throw new Error('Seu perfil não tem acesso ao dashboard.');
        if (!response.ok) { const error = await response.json().catch(() => ({})); throw new Error(error.mensagem || `Não foi possível consultar os dados (HTTP ${response.status}).`); }
        return response.json();
    }
    function row(body, values) { const tr = document.createElement('tr'); values.forEach(v => tr.append(text('td', v))); body.append(tr); }
    function render(data) {
        const k = data.indicadores;
        ['totalPedidos','finalizados','emAndamento','cancelados'].forEach(id => $(id).textContent = integer(k[id]));
        ['faturamento','ticketMedio','descontos'].forEach(id => $(id).textContent = money(k[id]));
        $('taxaCancelamento').textContent = Number(k.taxaCancelamento).toLocaleString('pt-BR') + '%';
        $('dash-vazio').hidden = k.totalPedidos > 0;
        $('atualizado').textContent = `Atualizado às ${data.atualizadoEm.substring(11,19)} · São Paulo`;
        const salesRows = $('tabela-vendas'); salesRows.replaceChildren();
        data.vendas.forEach(v => row(salesRows, [day(v.periodo), integer(v.pedidos), money(v.faturamento)]));
        if (salesChart) salesChart.destroy();
        if (statusChart) statusChart.destroy();
        if (typeof Chart !== 'undefined') {
            salesChart = new Chart($('grafico-vendas'), {type:'line', data:{labels:data.vendas.map(v => day(v.periodo)), datasets:[{label:'Faturamento', data:data.vendas.map(v => v.faturamento), borderColor:'#217454', backgroundColor:'rgba(33,116,84,.08)', borderWidth:2, pointRadius:data.vendas.length > 40 ? 0 : 3, lineTension:.2}]}, options:{responsive:true, maintainAspectRatio:false, legend:{display:false}, tooltips:{callbacks:{label:t => money(t.yLabel)}}, scales:{yAxes:[{ticks:{beginAtZero:true,callback:v => money(v)},gridLines:{color:'#edf1ed'}}],xAxes:[{gridLines:{display:false},ticks:{maxTicksLimit:10}}]}}});
            statusChart = new Chart($('grafico-status'), {type:'doughnut',data:{labels:data.situacoes.map(s=>s.descricao),datasets:[{data:data.situacoes.map(s=>s.quantidade),backgroundColor:colors,borderWidth:3,borderColor:'#fff'}]},options:{responsive:true,maintainAspectRatio:false,cutoutPercentage:72,legend:{display:false}}});
        }
        $('legenda-status').replaceChildren();
        data.situacoes.forEach((s,i) => { descriptions[s.status] = s.descricao; const li=text('li',''); const dot=text('span','','dash-dot'); dot.style.backgroundColor=colors[i]; li.append(dot,text('span',s.descricao),text('b',integer(s.quantidade))); $('legenda-status').append(li); });
        $('ranking').replaceChildren();
        if (!data.produtos.length) $('ranking').append(text('p','Nenhum produto vendido em pedidos finalizados.','dash-muted'));
        data.produtos.forEach(p => { const li=text('li',''); const info=text('div',p.nome); info.append(text('small',money(p.valorBruto)+' em itens')); const bar=document.createElement('progress'); bar.max=data.produtos[0].quantidade; bar.value=p.quantidade; bar.setAttribute('aria-label',`${p.nome}: ${p.quantidade} unidades`); info.append(bar); li.append(info,text('b',`${integer(p.quantidade)} un.`)); $('ranking').append(li); });
        $('pedidos-recentes').replaceChildren();
        data.pedidos.itens.forEach(p => row($('pedidos-recentes'), ['#'+p.id.substring(Math.max(0,p.id.length-8)), new Date(p.dataEntrada).toLocaleString('pt-BR'), p.tipoEntrega === 'ENTREGA' ? 'Entrega' : p.tipoEntrega === 'RETIRADA' ? 'Retirada' : 'Não informado', descriptions[p.status] || p.status, money(p.valorTotal)]));
        if (!data.pedidos.itens.length) row($('pedidos-recentes'),['Nenhum pedido nesta página.','','','','']);
        $('paginacao').textContent = `${data.pedidos.total ? page+1 : 0} de ${data.pedidos.totalPaginas} · ${data.pedidos.total} pedidos`;
        $('anterior').disabled = page === 0;
        $('proxima').disabled = page+1 >= data.pedidos.totalPaginas;
    }
    async function load() {
        if (busy) return;
        busy = true; $('dash-dados').setAttribute('aria-busy','true'); $('atualizar').disabled=true;
        const params = new URLSearchParams(applied); params.set('pagina',page);
        try { const data = await json('/api/dashboard?'+params); render(data); $('dash-erro').hidden=true; }
        catch(e) { $('dash-erro').textContent = (e.name === 'TimeoutError' ? 'A consulta demorou demais. Tente atualizar novamente.' : e.message || 'Falha de conexão.') + ' Os dados anteriores, se houver, não foram atualizados.'; $('dash-erro').hidden=false; }
        finally { busy=false; $('dash-dados').setAttribute('aria-busy','false'); $('atualizar').disabled=false; }
    }
    function filters() { const params=new URLSearchParams(new FormData($('filtros-dashboard'))); for (const [key,value] of [...params]) if (!value) params.delete(key); return params; }
    $('filtros-dashboard').addEventListener('submit',e=>{e.preventDefault();if(busy)return;page=0;applied=filters();load();});
    $('atualizar').addEventListener('click',()=>load());
    $('anterior').addEventListener('click',()=>{if(!busy&&page>0){page--;load();}});
    $('proxima').addEventListener('click',()=>{if(!busy){page++;load();}});
    const localDate=new Intl.DateTimeFormat('en-CA',{timeZone:'America/Sao_Paulo',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
    $('fim').value=localDate; $('inicio').value=localDate.substring(0,8)+'01'; applied=filters();
    async function init() {
        try { const f=await json('/api/dashboard/filtros'); f.status.forEach(s=>{const o=text('option',s.descricao);o.value=s.valor;$('status').append(o);}); f.tiposEntrega.forEach(s=>{const o=text('option',s==='ENTREGA'?'Entrega':'Retirada');o.value=s;$('tipoEntrega').append(o);}); }
        catch(e) { $('dash-erro').textContent=e.message; $('dash-erro').hidden=false; return; }
        await load();
    }
    init();
    setInterval(()=>{if($('automatico').checked&&!document.hidden)load();},60000);
})();
