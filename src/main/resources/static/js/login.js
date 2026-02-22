    const bodyEl = document.body;
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('overlay');
    const menuBtn = document.getElementById('menuBtn');
    const roleCitizen = document.getElementById('roleCitizen');
    const roleWard = document.getElementById('roleWard');
    const roleAdmin = document.getElementById('roleAdmin');
    const loginTitle = document.getElementById('loginTitle');
    const loginLabel = document.getElementById('loginLabel');
    const signupHint = document.getElementById('signupHint');

    function openMenu(){
        sidebar.classList.add('open');
        overlay.classList.add('show');
    }
    function closeMenu(){
        sidebar.classList.remove('open');
        overlay.classList.remove('show');
    }

    function setRole(role){
        bodyEl.classList.remove('role-ward', 'role-admin');

        if(role === 'admin'){
            // Administration login is a separate page (username + password)
            window.location.href = '/admin-login';
            return;
        }else if(role === 'ward'){
            // Ward login is a separate page (username + password)
            window.location.href = '/ward-login';
            return;
        }else{
            roleCitizen.classList.add('active');
            roleWard.classList.remove('active');
            roleAdmin.classList.remove('active');
            loginTitle.textContent = 'Citizen Login';
            loginLabel.innerHTML = 'Please enter your 10-digit phone number <span class="faint">+91 country code is applied by default</span>';
            signupHint.style.display = 'block';
        }
        closeMenu();
    }

    menuBtn.addEventListener('click', openMenu);
    overlay.addEventListener('click', closeMenu);
    roleCitizen.addEventListener('click', () => setRole('citizen'));
    roleWard.addEventListener('click', () => setRole('ward'));
    roleAdmin.addEventListener('click', () => setRole('admin'));

    const inputs = document.querySelectorAll('.phone-digit');
    const statusMessage = document.getElementById('statusMessage');

    inputs.forEach((input, index) => {
        input.addEventListener('input', (e) => {
            // Only allow digits
            input.value = input.value.replace(/[^0-9]/g, '');
            
            // Auto-advance to next box
            if (input.value.length === 1 && index < inputs.length - 1) {
                inputs[index + 1].focus();
            }

            // Check if all digits are filled
            const allFilled = Array.from(inputs).every(inp => inp.value.length === 1);
            if (allFilled) {
                const phone = Array.from(inputs).map(inp => inp.value).join('');
                checkPhoneAndRedirect(phone);
            }
        });

        input.addEventListener('keydown', (e) => {
            // Handle backspace
            if (e.key === 'Backspace' && input.value === '') {
                if (index > 0) {
                    inputs[index - 1].focus();
                }
            }
        });

        input.addEventListener('paste', (e) => {
            e.preventDefault();
            const paste = (e.clipboardData || window.clipboardData).getData('text');
            const digits = paste.replace(/\D/g, '').split('');
            
            digits.forEach((digit, i) => {
                if (i + index < inputs.length) {
                    inputs[i + index].value = digit;
                }
            });

            // Check if all filled after paste
            const allFilled = Array.from(inputs).every(inp => inp.value.length === 1);
            if (allFilled) {
                const phone = Array.from(inputs).map(inp => inp.value).join('');
                checkPhoneAndRedirect(phone);
            }
        });
    });

    async function checkPhoneAndRedirect(phone) {
        statusMessage.textContent = 'Checking...';
        statusMessage.className = 'status loading';

        try {
            const response = await fetch('/check-phone', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({ phone: phone })
            });

            const data = await response.json();

            if (data.exists) {
                // Phone registered - go to OTP page
                statusMessage.textContent = 'Phone found! Sending OTP...';
                statusMessage.className = 'status success';
                setTimeout(() => {
                    window.location.href = `/otp-page?phone=${phone}`;
                }, 500);
            } else {
                // Phone not registered - go to register page (only for citizen)
                const isCitizen = !bodyEl.classList.contains('role-ward') && !bodyEl.classList.contains('role-admin');
                if (isCitizen) {
                    statusMessage.textContent = 'Redirecting to registration...';
                    statusMessage.className = 'status';
                    setTimeout(() => {
                        window.location.href = `/register?phone=${phone}`;
                    }, 500);
                } else {
                    statusMessage.textContent = 'Not registered. Contact admin for access.';
                    statusMessage.className = 'status error';
                }
            }
        } catch (error) {
            statusMessage.textContent = 'Error checking phone. Please try again.';
            statusMessage.className = 'status error';
            console.error('Error:', error);
        }
    }

    // Auto-focus first input on page load
    window.addEventListener('load', () => {
        inputs[0].focus();
    });
