function isJpgFile(file){
    const type = (file.type || '').toLowerCase();
    const name = (file.name || '').toLowerCase();
    return type === 'image/jpeg' || type === 'image/jpg' || name.endsWith('.jpg') || name.endsWith('.jpeg');
}

function handlePhoto(event){
    const file = event.target.files[0];
    if (!file) return;
    if (!isJpgFile(file)) {
        document.getElementById('photoBase64').value = '';
        event.target.value = '';
        alert('Only JPG format is allowed for profile photo.');
        return;
    }
    const reader = new FileReader();
    reader.onload = function(e){
        document.getElementById('photoPreview').src = e.target.result;
        document.getElementById('photoBase64').value = e.target.result;
    };
    reader.readAsDataURL(file);
}

async function sendWardPasswordOtp() {
    const status = document.getElementById('passwordOtpStatus');
    if (status) {
        status.textContent = 'Sending OTP...';
        status.style.color = '#1f6f4a';
    }

    try {
        const response = await fetch('/ward-profile/send-password-otp', {
            method: 'POST',
            headers: {
                'X-Requested-With': 'XMLHttpRequest'
            }
        });

        const data = await response.json();
        const message = data.message || (data.success ? 'OTP sent successfully.' : 'Unable to send OTP.');
        if (status) {
            status.textContent = message;
            status.style.color = data.success ? '#1f6f4a' : '#b53838';
        } else {
            alert(message);
        }
    } catch (error) {
        if (status) {
            status.textContent = 'Unable to send OTP. Please try again.';
            status.style.color = '#b53838';
        } else {
            alert('Unable to send OTP. Please try again.');
        }
    }
}
