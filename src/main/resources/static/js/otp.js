const inputs = document.querySelectorAll('.otp-box');
const form = document.querySelector('form');
const hiddenOtpInput = document.getElementById('otpInput');

if (form && hiddenOtpInput && inputs.length > 0) {
    inputs.forEach((input, index) => {
        input.addEventListener('keyup', (e) => {
            if (e.key === 'Backspace') {
                input.value = '';
                if (index > 0) {
                    inputs[index - 1].focus();
                }
            } else if (/[0-9]/.test(e.key)) {
                input.value = e.key;
                if (index < inputs.length - 1) {
                    inputs[index + 1].focus();
                }
            }
        });

        input.addEventListener('paste', (e) => {
            const paste = (e.clipboardData || window.clipboardData).getData('text');
            const digits = paste.replace(/\D/g, '').split('');
            digits.forEach((digit, i) => {
                if (i < inputs.length) {
                    inputs[i].value = digit;
                }
            });
        });
    });

    form.addEventListener('submit', () => {
        const otp = Array.from(inputs).map(input => input.value).join('');
        hiddenOtpInput.value = otp;
    });
}
