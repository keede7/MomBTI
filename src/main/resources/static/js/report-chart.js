/*
 * 변화 그래프 (점진적 향상, ADR-014). 서버가 준 data-chart JSON(ChartDataView)만 그린다. 여기서는 아무것도 계산하지 않는다.
 * Chart.js(cdnjs)가 없거나 JS가 꺼져 있으면 아래 표가 같은 정보를 보여 준다.
 * 기간 규칙은 서버의 ChartWindowPolicy 결과(window, sixMonthToggle, goalWeek)를 그대로 따른다 (ADR-021).
 */
(function () {
    'use strict';

    var canvas = document.getElementById('weight-chart');
    if (!canvas || typeof window.Chart === 'undefined') {
        return;
    }

    var data;
    try {
        data = JSON.parse(canvas.dataset.chart);
    } catch (e) {
        return;
    }

    var SCENARIO_STYLE = {
        DIET_ONLY: {color: '--s0', dash: [], width: 2},
        EX3: {color: '--s3', dash: [6, 4], width: 2},
        EX4: {color: '--s4', dash: [], width: 3.5},
        EX5: {color: '--s5', dash: [2, 3], width: 2}
    };

    var state = {
        weeks: data.defaultWeeks,
        adherence: false,
        visible: {}
    };
    data.series.forEach(function (s) {
        state.visible[s.key] = s.visibleByDefault;
    });

    var chart = null;

    function token(name) {
        return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
    }

    function withAlpha(hex, alpha) {
        var value = hex.replace('#', '');
        if (value.length === 3) {
            value = value.split('').map(function (c) { return c + c; }).join('');
        }
        var n = parseInt(value, 16);
        return 'rgba(' + ((n >> 16) & 255) + ',' + ((n >> 8) & 255) + ',' + (n & 255) + ',' + alpha + ')';
    }

    /** 서버 배열을 현재 기간(12주/26주) 안의 {x: 주차, y: kg} 점으로 옮긴다. */
    function points(values) {
        var result = [];
        data.weeks.forEach(function (week, i) {
            if (week <= state.weeks) {
                result.push({x: week, y: Number(values[i])});
            }
        });
        return result;
    }

    function dateOf(week) {
        var index = data.weeks.indexOf(week);
        return index >= 0 ? data.dates[index] : '';
    }

    function goalMarkerVisible() {
        return data.window === 'GOAL_IN_VIEW'
            || (data.window === 'GOAL_WITHIN_SIX_MONTHS' && state.weeks >= data.goalWeek);
    }

    /** 그래프 오른쪽 끝 또는 도달 지점에 붙는 안내 문구. */
    function edgeLabel() {
        if (data.window === 'GOAL_WITHIN_SIX_MONTHS' && state.weeks < data.goalWeek) {
            return '목표까지 약 ' + data.goalWeek + '주';
        }
        if (data.window === 'GOAL_BEYOND_SIX_MONTHS') {
            return '6개월 안에는 어려워요';
        }
        return '';
    }

    var labelPlugin = {
        id: 'mombtiLabels',
        afterDatasetsDraw: function (c) {
            var ctx = c.ctx;
            var area = c.chartArea;
            ctx.save();
            ctx.font = '600 12px "IBM Plex Sans KR", sans-serif';
            ctx.fillStyle = token('--ink');
            if (goalMarkerVisible()) {
                var x = c.scales.x.getPixelForValue(data.goalWeek);
                var y = c.scales.y.getPixelForValue(Number(data.targetKg));
                ctx.textAlign = x > area.right - 60 ? 'right' : 'center';
                ctx.fillText('목표 도달', x, y - 10);
            }
            var text = edgeLabel();
            if (text) {
                ctx.textAlign = 'right';
                ctx.fillText(text, area.right - 4, area.top + 14);
            }
            ctx.restore();
        }
    };

    function datasets() {
        var sets = [];
        var dietColor = token('--s0');

        // 개인차 밴드 (식단만, 기초대사량 ±10%): 상단과 하단 사이를 15% 투명도로 채운다
        sets.push({
            kind: 'band', label: '개인차 범위 상단', data: points(data.bandUpperKg),
            borderWidth: 0, pointRadius: 0, fill: false, hidden: !state.visible.DIET_ONLY
        });
        sets.push({
            kind: 'band', label: '개인차 범위 하단', data: points(data.bandLowerKg),
            borderWidth: 0, pointRadius: 0, fill: '-1', backgroundColor: withAlpha(dietColor, 0.15),
            hidden: !state.visible.DIET_ONLY
        });

        data.series.forEach(function (s) {
            var style = SCENARIO_STYLE[s.key];
            var color = token(style.color);
            sets.push({
                kind: 'series', label: s.label, data: points(s.weightsKg),
                borderColor: color, backgroundColor: color, borderWidth: style.width, borderDash: style.dash,
                pointRadius: 2, pointHoverRadius: 4, fill: false, tension: 0.2, hidden: !state.visible[s.key]
            });
        });

        data.series.forEach(function (s) {
            var color = token(SCENARIO_STYLE[s.key].color);
            sets.push({
                kind: 'adherence', label: s.label + ' · 현실 보정(가정)', data: points(s.adherenceWeightsKg),
                borderColor: withAlpha(color, 0.6), borderWidth: 1.5, borderDash: [1, 4],
                pointRadius: 0, fill: false, tension: 0.2, hidden: !(state.adherence && state.visible[s.key])
            });
        });

        if (data.showTarget) {
            sets.push({
                kind: 'target', label: '목표', data: [{x: 0, y: Number(data.targetKg)}, {x: state.weeks, y: Number(data.targetKg)}],
                borderColor: token('--target'), borderWidth: 1.5, borderDash: [4, 4], pointRadius: 0, fill: false
            });
        }
        if (goalMarkerVisible()) {
            sets.push({
                kind: 'marker', label: '목표 도달', data: [{x: data.goalWeek, y: Number(data.targetKg)}],
                borderColor: token('--target'), backgroundColor: token('--paper'), borderWidth: 2,
                pointRadius: 5, showLine: false
            });
        }
        return sets;
    }

    function options() {
        var ink3 = token('--ink-3');
        var line = token('--line');
        return {
            animation: false,
            responsive: true,
            maintainAspectRatio: false,
            interaction: {mode: 'nearest', axis: 'x', intersect: false},
            scales: {
                x: {
                    type: 'linear', min: 0, max: state.weeks,
                    ticks: {stepSize: state.weeks > data.defaultWeeks ? 4 : 2, color: ink3,
                        callback: function (value) { return value === 0 ? '지금' : value + '주'; }},
                    grid: {color: line}
                },
                y: {
                    ticks: {color: ink3, callback: function (value) { return value + 'kg'; }},
                    grid: {color: line}
                }
            },
            plugins: {
                legend: {display: false},
                tooltip: {
                    filter: function (item) {
                        var kind = item.dataset.kind;
                        return kind === 'series' || kind === 'adherence';
                    },
                    callbacks: {
                        title: function (items) {
                            if (!items.length) {
                                return '';
                            }
                            var week = items[0].parsed.x;
                            return (week === 0 ? '지금' : week + '주 후') + ' · ' + dateOf(week);
                        },
                        label: function (item) {
                            return item.dataset.label + ': ' + item.parsed.y.toFixed(1) + 'kg';
                        }
                    }
                }
            }
        };
    }

    function render() {
        if (chart) {
            chart.destroy();
        }
        chart = new window.Chart(canvas, {
            type: 'line',
            data: {datasets: datasets()},
            options: options(),
            plugins: [labelPlugin]
        });
    }

    function buildLegend() {
        var legend = document.querySelector('[data-legend]');
        data.series.forEach(function (s) {
            var button = document.createElement('button');
            button.type = 'button';
            button.className = 'toggle';
            button.setAttribute('aria-pressed', String(state.visible[s.key]));
            var swatch = document.createElement('span');
            swatch.className = 'toggle-swatch toggle-swatch-' + s.key;
            swatch.setAttribute('aria-hidden', 'true');
            button.appendChild(swatch);
            button.appendChild(document.createTextNode(s.label));
            button.addEventListener('click', function () {
                state.visible[s.key] = !state.visible[s.key];
                button.setAttribute('aria-pressed', String(state.visible[s.key]));
                render();
            });
            legend.appendChild(button);
        });
    }

    function bindToggle(id, onToggle) {
        var button = document.getElementById(id);
        if (!button) {
            return;
        }
        button.addEventListener('click', function () {
            var pressed = button.getAttribute('aria-pressed') !== 'true';
            button.setAttribute('aria-pressed', String(pressed));
            onToggle(pressed);
            render();
        });
    }

    buildLegend();
    // 6개월 토글은 서버가 sixMonthToggle일 때만 렌더한다 (E-18 숨김, E-19·E-20 노출)
    bindToggle('toggle-six-months', function (pressed) {
        state.weeks = pressed ? data.extendedWeeks : data.defaultWeeks;
    });
    bindToggle('toggle-adherence', function (pressed) {
        state.adherence = pressed;
    });

    document.querySelector('[data-chart-controls]').hidden = false;
    document.querySelector('[data-chart-box]').hidden = false;
    render();

    // 다크모드 전환 시 토큰 색을 다시 읽는다
    if (window.matchMedia) {
        var scheme = window.matchMedia('(prefers-color-scheme: dark)');
        if (scheme.addEventListener) {
            scheme.addEventListener('change', render);
        }
    }
})();
