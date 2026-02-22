function toggleMenu(){
    sidebar.classList.toggle("open");
    overlay.classList.toggle("show");
}

function isJpgFile(file){
    const type = (file.type || '').toLowerCase();
    const name = (file.name || '').toLowerCase();
    return type === 'image/jpeg' || type === 'image/jpg' || name.endsWith('.jpg') || name.endsWith('.jpeg');
}

function handleProfilePhoto(event){
    const file = event.target.files[0];
    if (!file) return;

    const hidden = document.getElementById('photoBase64');
    const preview = document.getElementById('profilePhotoPreview');

    if (!isJpgFile(file)) {
        if (hidden) hidden.value = '';
        event.target.value = '';
        alert('Only JPG format is allowed for profile photo.');
        return;
    }

    const reader = new FileReader();
    reader.onload = function(e){
        if (preview) preview.src = e.target.result;
        if (hidden) hidden.value = e.target.result;
    };
    reader.readAsDataURL(file);
}

async function sendPasswordOtp() {
    const status = document.getElementById('passwordOtpStatus');
    if (status) {
        status.textContent = 'Sending OTP...';
        status.style.color = '#1f6f4a';
    }

    try {
        const response = await fetch('/profile/send-password-otp', {
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
