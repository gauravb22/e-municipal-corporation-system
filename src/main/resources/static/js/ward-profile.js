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
