    const inputs = document.querySelectorAll('.otp-box');
    
    inputs.forEach((input, index) => {
        input.addEventListener('keyup', (e) => {
            if (e.key === 'Backspace') {
                input.value = '';
                if (index > 0) inputs[index - 1].focus();
            } else if (/[0-9]/.test(e.key)) {
                input.value = e.key;
                if (index < inputs.length - 1) inputs[index + 1].focus();
            }
        });
        
        input.addEventListener('paste', (e) => {
            const paste = (e.clipboardData || window.clipboardData).getData('text');
            const digits = paste.replace(/\D/g, '').split('');
            digits.forEach((digit, i) => {
                if (i < inputs.length) inputs[i].value = digit;
            });
        });
    });
    
    document.querySelector('form').addEventListener('submit', (e) => {
        const otp = Array.from(inputs).map(input => input.value).join('');
        document.getElementById('otpInput').value = otp;
    });
