const bodyEl = document.body;
const sidebar = document.getElementById('sidebar');
const overlay = document.getElementById('overlay');
const menuBtn = document.getElementById('menuBtn');
const roleCitizen = document.getElementById('roleCitizen');
const roleWard = document.getElementById('roleWard');
const roleAdmin = document.getElementById('roleAdmin');
const loginTitle = document.getElementById('loginTitle');
const loginSubTitle = document.getElementById('loginSubTitle');
const loginLabel = document.getElementById('loginLabel');
const statusMessage = document.getElementById('statusMessage');
const inputs = document.querySelectorAll('.phone-digit');

const defaultCitizenLabel = loginLabel ? loginLabel.innerHTML : '';
const defaultSubTitle = loginSubTitle ? loginSubTitle.textContent : '';

function openMenu() {
    if (!sidebar || !overlay) {
        return;
    }
    sidebar.classList.add('open');
    overlay.classList.add('show');
}

function closeMenu() {
    if (!sidebar || !overlay) {
        return;
    }
    sidebar.classList.remove('open');
    overlay.classList.remove('show');
}

function setStatus(text, typeClass) {
    if (!statusMessage) {
        return;
    }
    statusMessage.textContent = text;
    statusMessage.className = `status ${typeClass || ''}`.trim();
}

function setRole(role) {
    if (!bodyEl) {
        return;
    }

    if (role === 'admin') {
        window.location.href = '/admin-login';
        return;
    }
    if (role === 'ward') {
        window.location.href = '/ward-login';
        return;
    }

    bodyEl.classList.remove('role-ward', 'role-admin');
    bodyEl.classList.add('role-citizen');

    if (roleCitizen) roleCitizen.classList.add('active');
    if (roleWard) roleWard.classList.remove('active');
    if (roleAdmin) roleAdmin.classList.remove('active');

    if (loginTitle) loginTitle.textContent = 'Citizen Login';
    if (loginSubTitle) loginSubTitle.textContent = defaultSubTitle;
    if (loginLabel) loginLabel.innerHTML = defaultCitizenLabel;

    setStatus('', '');
    inputs.forEach((input) => {
        input.value = '';
    });
    if (inputs.length > 0) {
        inputs[0].focus();
    }
    closeMenu();
}

if (menuBtn) menuBtn.addEventListener('click', openMenu);
if (overlay) overlay.addEventListener('click', closeMenu);
if (roleCitizen) roleCitizen.addEventListener('click', () => setRole('citizen'));
if (roleWard) roleWard.addEventListener('click', () => setRole('ward'));
if (roleAdmin) roleAdmin.addEventListener('click', () => setRole('admin'));

function collectPhoneDigits() {
    return Array.from(inputs).map((inp) => inp.value).join('');
}

function allDigitsFilled() {
    return Array.from(inputs).every((inp) => inp.value.length === 1);
}

inputs.forEach((input, index) => {
    input.addEventListener('input', () => {
        input.value = input.value.replace(/[^0-9]/g, '');
        if (input.value.length === 1 && index < inputs.length - 1) {
            inputs[index + 1].focus();
        }

        if (allDigitsFilled()) {
            checkPhoneAndRedirect(collectPhoneDigits());
        }
    });

    input.addEventListener('keydown', (e) => {
        if (e.key === 'Backspace' && input.value === '' && index > 0) {
            inputs[index - 1].focus();
        }
    });

    input.addEventListener('paste', (e) => {
        e.preventDefault();
        const paste = (e.clipboardData || window.clipboardData).getData('text');
        const digits = paste.replace(/\D/g, '').slice(0, inputs.length).split('');

        digits.forEach((digit, i) => {
            inputs[i].value = digit;
        });

        if (allDigitsFilled()) {
            checkPhoneAndRedirect(collectPhoneDigits());
        }
    });
});

async function checkPhoneAndRedirect(phone) {
    setStatus('Checking...', 'loading');

    try {
        const response = await fetch('/check-phone', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({ phone })
        });

        const data = await response.json();
        if (data.exists) {
            setStatus('Phone found! Sending OTP...', 'success');
            setTimeout(() => {
                window.location.href = `/otp-page?phone=${phone}`;
            }, 300);
            return;
        }

        setStatus('Redirecting to registration...', '');
        setTimeout(() => {
            window.location.href = `/register?phone=${phone}`;
        }, 300);
    } catch (error) {
        setStatus('Error checking phone. Please try again.', 'error');
        console.error('Error:', error);
    }
}

window.addEventListener('load', () => {
    setRole('citizen');
});
