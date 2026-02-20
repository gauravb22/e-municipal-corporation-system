function isJpgFile(file) {
    const type = (file.type || '').toLowerCase();
    const name = (file.name || '').toLowerCase();
    return type === 'image/jpeg' || type === 'image/jpg' || name.endsWith('.jpg') || name.endsWith('.jpeg');
}

function captureDonePhoto() {
    document.getElementById('donePhotoInput').click();
}

function handleDonePhotoCapture(event) {
    const file = event.target.files[0];
    if (!file) {
        return;
    }
    if (!isJpgFile(file)) {
        event.target.value = '';
        alert('Only JPG format is allowed for completion photo.');
        return;
    }

    const preview = document.getElementById('donePhotoPreview');
    preview.src = URL.createObjectURL(file);
    preview.style.display = 'block';
    document.getElementById('donePhotoCapturedMsg').style.display = 'block';
    document.getElementById('markDoneBtn').disabled = false;

    const now = new Date();
    document.getElementById('donePhotoTimestamp').value = now.toISOString();

    if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
            function(position) {
                const lat = position.coords.latitude.toFixed(6);
                const lon = position.coords.longitude.toFixed(6);
                document.getElementById('donePhotoLatitude').value = lat;
                document.getElementById('donePhotoLongitude').value = lon;
                document.getElementById('donePhotoLocation').value = `${lat}, ${lon}`;
            },
            function() {
                document.getElementById('donePhotoLocation').value = 'Location not available';
            }
        );
    } else {
        document.getElementById('donePhotoLocation').value = 'Location not available';
    }
}

const completeForm = document.getElementById('completeForm');
if (completeForm) {
    completeForm.addEventListener('submit', function(e) {
        const input = document.getElementById('donePhotoInput');
        if (!input || !input.files || input.files.length === 0) {
            e.preventDefault();
            alert('Please capture a completion photo before marking as done.');
        }
    });
}
