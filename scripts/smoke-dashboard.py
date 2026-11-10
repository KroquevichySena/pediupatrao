"""Smoke HTTP com login real, sem dependências externas. Execute contra o perfil demo.

python scripts/smoke-dashboard.py [http://127.0.0.1:8080]
SMOKE_PASSWORD permite usar uma senha de demonstração diferente, sem gravá-la em arquivo.
"""
import http.cookiejar
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import date, timedelta
from decimal import Decimal

BASE = sys.argv[1].rstrip('/') if len(sys.argv) > 1 else 'http://127.0.0.1:8080'


def request(opener, path, expected=200, data=None):
    try:
        response = opener.open(BASE + path, data=data, timeout=25)
    except urllib.error.HTTPError as error:
        response = error
    assert response.code == expected, (path, response.code, expected)
    return response.read().decode('utf-8')


def session(username):
    opener = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
    html = request(opener, '/login')
    token = re.search(r'name="_csrf"[^>]*value="([^"]+)"', html).group(1)
    form = urllib.parse.urlencode({'username': username, 'password': os.environ.get('SMOKE_PASSWORD', 'pizza@2026'), '_csrf': token}).encode()
    result = request(opener, '/login', data=form)
    assert 'name="username"' not in result, 'Login falhou'
    return opener


anonymous = urllib.request.build_opener()
request(anonymous, '/api/dashboard', 401)
print('PASS: API sem sessão retorna 401')
manager = session('gerente01')
html = request(manager, '/dashboard')
assert 'Visão da operação' in html and 'grafico-vendas' in html
for asset in ['/js/dashboard.js', '/css/dashboard.css', '/plugins/chart.js/Chart.min.js']:
    assert len(request(manager, asset)) > 100
print('PASS: login real de GERENTE, template e recursos locais retornam 200')
filters = json.loads(request(manager, '/api/dashboard/filtros'))
assert len(filters['status']) == 7
today = date.today()
query = urllib.parse.urlencode({'inicio': today - timedelta(days=27), 'fim': today, 'agrupamento': 'dia'})
raw = request(manager, '/api/dashboard?' + query)
data = json.loads(raw, parse_float=Decimal)
k = data['indicadores']
assert k['totalPedidos'] > 0, 'Execute no perfil demo para validar a massa fictícia'
assert sum(s['quantidade'] for s in data['situacoes']) == k['totalPedidos']
assert sum(v['faturamento'] for v in data['vendas']) == k['faturamento']
assert k['finalizados'] + k['cancelados'] + k['emAndamento'] == k['totalPedidos']
assert len(data['vendas']) == 28
print('PASS: JSON real com indicadores, série de 28 dias e status consistentes')
empty = json.loads(request(manager, '/api/dashboard?inicio=2000-01-01&fim=2000-01-02'))
assert empty['indicadores']['totalPedidos'] == 0 and empty['produtos'] == []
print('PASS: período vazio retorna 200 e valores zerados')
request(manager, '/api/dashboard?status=INVALIDO', 400)
request(manager, '/api/dashboard?inicio=2026-11-10&fim=2026-11-01', 422)
print('PASS: filtros inválidos retornam 400 e 422')
attendant = session('atendente01')
request(attendant, '/api/dashboard', 403)
request(attendant, '/dashboard', 403)
print('PASS: login real de ATENDENTE bloqueado na API e página com 403')
admin = session('admin')
request(admin, '/api/dashboard/filtros')
print('PASS: login real de ADMIN autorizado')
print('SMOKE HTTP OK')
