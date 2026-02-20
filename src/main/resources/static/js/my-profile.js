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
