const requiresPhoto = document.body.getAttribute('data-requires-photo') === 'true';
const notComingDateInput = document.querySelector('input[name="notComingFromDate"]');
const locationInput = document.querySelector('input[name="location"]');

function updateTimestamp() {
    const now = new Date();
    const timestamp = now.toLocaleString('en-US', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: true
    });
    const timestampDisplay = document.getElementById('timestampDisplay');
    const photoTimestamp = document.getElementById('photoTimestamp');
    if (timestampDisplay) {
        timestampDisplay.textContent = timestamp;
    }
    if (photoTimestamp) {
        photoTimestamp.value = now.toISOString();
    }
}

if (requiresPhoto) {
    setInterval(updateTimestamp, 1000);
    updateTimestamp();
}

function getLocation() {
    if (!navigator.geolocation) {
        return;
    }
    navigator.geolocation.getCurrentPosition(
        function(position) {
            const lat = position.coords.latitude.toFixed(6);
            const lon = position.coords.longitude.toFixed(6);
            const accuracy = position.coords.accuracy.toFixed(0);

            const photoLatitude = document.getElementById('photoLatitude');
            const photoLongitude = document.getElementById('photoLongitude');
            const photoLocation = document.getElementById('photoLocation');
            const locationDisplay = document.getElementById('locationDisplay');

            if (photoLatitude) {
                photoLatitude.value = lat;
            }
            if (photoLongitude) {
                photoLongitude.value = lon;
            }
            if (photoLocation) {
                photoLocation.value = lat + ", " + lon;
            }
            if (locationDisplay) {
                locationDisplay.textContent = lat + ", " + lon + " (+/-" + accuracy + "m)";
            }
        },
        function(error) {
            console.error('Geolocation error:', error);
            const photoLatitude = document.getElementById('photoLatitude');
            const photoLongitude = document.getElementById('photoLongitude');
            const photoLocation = document.getElementById('photoLocation');
            const locationDisplay = document.getElementById('locationDisplay');
            if (photoLatitude) {
                photoLatitude.value = '';
            }
            if (photoLongitude) {
                photoLongitude.value = '';
            }
            if (photoLocation) {
                photoLocation.value = '';
            }
            if (locationDisplay) {
                locationDisplay.textContent = 'Location required. Please enable GPS.';
            }
        }
    );
}

if (requiresPhoto) {
    getLocation();
}

function capturePhoto() {
    getLocation();
    const photoInput = document.getElementById('photoInput');
    if (photoInput) {
        photoInput.click();
    }
}

function isJpgFile(file) {
    const type = (file.type || '').toLowerCase();
    const name = (file.name || '').toLowerCase();
    return type === 'image/jpeg' || type === 'image/jpg' || name.endsWith('.jpg') || name.endsWith('.jpeg');
}

function handlePhotoCapture(event) {
    const file = event.target.files[0];
    if (!file) {
        return;
    }
    if (!isJpgFile(file)) {
        const photoBase64 = document.getElementById('photoBase64');
        const photoPreview = document.getElementById('photoPreview');
        const photoCapturedMsg = document.getElementById('photoCapturedMsg');
        if (photoBase64) {
            photoBase64.value = '';
        }
        if (photoPreview) {
            photoPreview.removeAttribute('src');
            photoPreview.classList.remove('active');
        }
        if (photoCapturedMsg) {
            photoCapturedMsg.classList.remove('show');
        }
        alert('Only JPG format is allowed for complaint photo.');
        event.target.value = '';
        return;
    }

    const reader = new FileReader();
    reader.onload = function(e) {
        const photoPreview = document.getElementById('photoPreview');
        const photoCapturedMsg = document.getElementById('photoCapturedMsg');
        const photoBase64 = document.getElementById('photoBase64');

        if (photoPreview) {
            photoPreview.src = e.target.result;
            photoPreview.classList.add('active');
        }
        if (photoCapturedMsg) {
            photoCapturedMsg.classList.add('show');
        }
        if (photoBase64) {
            photoBase64.value = e.target.result;
        }
    };
    reader.readAsDataURL(file);
}

function hasCapturedLocation() {
    const photoLocation = document.getElementById('photoLocation');
    const photoLatitude = document.getElementById('photoLatitude');
    const photoLongitude = document.getElementById('photoLongitude');
    const locationValue = photoLocation && photoLocation.value ? photoLocation.value.trim() : '';
    const latValue = photoLatitude && photoLatitude.value ? photoLatitude.value.trim() : '';
    const lonValue = photoLongitude && photoLongitude.value ? photoLongitude.value.trim() : '';

    if (!locationValue || locationValue.toLowerCase() === 'location not available') {
        return false;
    }
    if (!latValue || !lonValue) {
        return false;
    }
    if (latValue === '0' && lonValue === '0') {
        return false;
    }
    return true;
}

const complaintForm = document.querySelector('form');
if (complaintForm) {
    complaintForm.addEventListener('submit', function(e) {
        if (!locationInput || !locationInput.value || !locationInput.value.trim()) {
            e.preventDefault();
            alert('Please enter complaint location before submitting.');
            return false;
        }

        const photoBase64 = document.getElementById('photoBase64');
        if (requiresPhoto && (!photoBase64 || !photoBase64.value)) {
            e.preventDefault();
            alert('Please capture a photo before submitting.');
            return false;
        }
        if (requiresPhoto && !hasCapturedLocation()) {
            e.preventDefault();
            alert('Location is mandatory for photo complaints. Please enable GPS and capture photo again.');
            return false;
        }
        if (!requiresPhoto && (!notComingDateInput || !notComingDateInput.value)) {
            e.preventDefault();
            alert('Please select not coming from date before submitting.');
            return false;
        }
        return true;
    });
}
