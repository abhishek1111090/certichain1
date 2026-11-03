// Small progressive enhancements. Kept in a file (not inline) so the strict CSP applies.
document.addEventListener('DOMContentLoaded', function () {
    document.querySelectorAll('[data-print]').forEach(function (btn) {
        btn.addEventListener('click', function () { window.print(); });
    });

    document.querySelectorAll('form[data-confirm]').forEach(function (form) {
        form.addEventListener('submit', function (e) {
            if (!window.confirm(form.getAttribute('data-confirm'))) {
                e.preventDefault();
            }
        });
    });

    document.querySelectorAll('[data-copy]').forEach(function (btn) {
        btn.addEventListener('click', function () {
            var text = btn.getAttribute('data-copy');
            if (text.indexOf('/') === 0) {
                text = window.location.origin + text;
            }
            navigator.clipboard.writeText(text).then(function () {
                var old = btn.textContent;
                btn.textContent = 'Copied';
                setTimeout(function () { btn.textContent = old; }, 1400);
            });
        });
    });

    // Upper-case certificate numbers as they are typed.
    document.querySelectorAll('input[data-upper]').forEach(function (input) {
        input.addEventListener('input', function () {
            var pos = input.selectionStart;
            input.value = input.value.toUpperCase();
            input.setSelectionRange(pos, pos);
        });
    });
});
