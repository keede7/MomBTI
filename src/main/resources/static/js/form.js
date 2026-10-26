/*
 * 입력 화면 점진적 향상: 첫 오류 포커스, 비어 있는 선택 항목 안내, BMI·목표 체중 미리보기.
 * 미리보기는 표시 전용이다. 계산 결과를 서버로 보내지 않고, 리포트의 모든 수치는 서버가 다시 계산한다.
 */
(function () {
    'use strict';

    var form = document.getElementById('report-form');
    if (!form) {
        return;
    }

    var OPTIONAL_LABELS = {activityLevel: '활동량', targetWeightKg: '목표 체중', bodyFatPercent: '체지방률'};

    function focusFirstError() {
        var invalid = form.querySelector('[aria-invalid="true"], fieldset .field-error');
        if (!invalid) {
            return;
        }
        var target = invalid.matches('input, select') ? invalid : invalid.closest('fieldset').querySelector('input');
        if (target) {
            target.focus();
        }
    }

    /** "72,5", "175cm", 전각 숫자 같은 입력을 미리보기용으로만 읽는다. 서버는 따로 정규화한다. */
    function readNumber(name) {
        var field = form.elements[name];
        if (!field || !field.value) {
            return NaN;
        }
        var text = field.value
            .replace(/[０-９]/g, function (ch) { return String.fromCharCode(ch.charCodeAt(0) - 0xFEE0); })
            .replace(/[．,，]/g, '.')
            .replace(/[^0-9.]/g, '');
        return text ? Number(text) : NaN;
    }

    function show(element, text) {
        element.textContent = text;
        element.hidden = !text;
    }

    function updatePreviews() {
        var heightM = readNumber('heightCm') / 100;
        var weight = readNumber('weightKg');
        var target = readNumber('targetWeightKg');
        var valid = heightM >= 1.2 && heightM <= 2.3 && weight >= 30 && weight <= 300;

        show(document.getElementById('bmi-preview'),
            valid ? '지금 BMI 약 ' + (weight / (heightM * heightM)).toFixed(1) : '');

        var targetText = '';
        if (valid && target >= 30 && target <= 300) {
            var diff = target - weight;
            var sign = diff < 0 ? '−' : (diff > 0 ? '+' : '');
            targetText = 'BMI ' + (target / (heightM * heightM)).toFixed(1)
                + ' · 지금보다 ' + sign + Math.abs(diff).toFixed(1) + 'kg';
        }
        show(document.getElementById('target-preview'), targetText);
    }

    function updateOptionalHint() {
        var hint = document.getElementById('optional-hint');
        var empty = Object.keys(OPTIONAL_LABELS).filter(function (name) {
            var field = form.elements[name];
            return field && !field.value;
        }).map(function (name) { return OPTIONAL_LABELS[name]; });
        hint.textContent = empty.length === 0
            ? '선택 항목을 모두 넣었어요. 더 맞춤형으로 분석해 드려요.'
            : empty.join(', ') + '이(가) 비어 있어요. 비워 두면 근거 있는 기본값으로 추정해요. 그대로 분석해도 괜찮아요.';
    }

    form.addEventListener('input', function () {
        updatePreviews();
        updateOptionalHint();
    });
    form.addEventListener('change', updateOptionalHint);

    focusFirstError();
    updatePreviews();
    updateOptionalHint();
})();
