    const form = document.getElementById('adminLoginForm');
    const statusMessage = document.getElementById('statusMessage');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();

        const username = document.getElementById('username').value.trim();
        const password = document.getElementById('password').value;

        statusMessage.textContent = 'Checking...';
        statusMessage.className = 'status';

        try {
            const response = await fetch('/admin-login', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ username, password })
            });

            const data = await response.json();

            if (data.success) {
                window.location.href = data.redirect;
            } else {
                statusMessage.textContent = data.message || 'Invalid username or password';
                statusMessage.className = 'status error';
            }
        } catch (err) {
            statusMessage.textContent = 'Login failed. Try again.';
            statusMessage.className = 'status error';
        }
    });
