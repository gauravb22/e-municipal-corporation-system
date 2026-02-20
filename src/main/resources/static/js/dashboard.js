const sidebar = document.getElementById("sidebar");
const overlay = document.getElementById("overlay");
const notifBell = document.getElementById("notifBell");
const notifPanel = document.getElementById("notifPanel");

function toggleMenu(){
    sidebar.classList.toggle("open");
    overlay.classList.toggle("show");
}

function toggleNotifications(event){
    if (event) {
        event.stopPropagation();
    }
    if (!notifPanel) {
        return;
    }
    notifPanel.classList.toggle("show");
}

document.addEventListener("click", function (event) {
    if (!notifPanel || !notifBell) {
        return;
    }
    if (notifPanel.contains(event.target) || notifBell.contains(event.target)) {
        return;
    }
    notifPanel.classList.remove("show");
});


const adminSlides = [
    {
        role: "Municipal Commissioner",
        name: "Shri Ajitji Pawar",
        desc: "City administration and civic service oversight.",
        photo: "/images/Commissoner.png",
        alt: "Municipal Commissioner"
    },
    {
        role: "Mayor (Mahapor)",
        name: "Shri Jayprakash Patil",
        desc: "Elected representative leading civic initiatives.",
        photo: "/images/Mayor.png",
        alt: "Mayor"
    },
    {
        role: "Deputy Mayor",
        name: "Shri Rekha Sawant",
        desc: "Supports municipal programs and ward coordination.",
        photo: "/images/Deupty_Mayor.png",
        alt: "Deputy Mayor"
    }
];

let adminIndex = 0;
const adminRole = document.getElementById("adminRole");
const adminName = document.getElementById("adminName");
const adminDesc = document.getElementById("adminDesc");
const adminPhoto = document.getElementById("adminPhoto");
const adminIndicator = document.getElementById("adminIndicator");

function renderAdminSlide(index){
    const slide = adminSlides[index];
    adminRole.textContent = slide.role;
    adminName.textContent = slide.name;
    adminDesc.textContent = slide.desc;
    adminPhoto.src = slide.photo;
    adminPhoto.alt = slide.alt;
    adminIndicator.textContent = `${index + 1} / ${adminSlides.length}`;
}

if (adminRole && adminName && adminDesc && adminPhoto && adminIndicator) {
    renderAdminSlide(adminIndex);
    setInterval(() => {
        adminIndex = (adminIndex + 1) % adminSlides.length;
        renderAdminSlide(adminIndex);
    }, 3500);
}

const toast = document.getElementById("complaintToast");
if (toast) {
    setTimeout(() => {
        toast.style.animation = "toast-out .3s ease forwards";
    }, 3000);
    setTimeout(() => {
        if (toast && toast.parentNode) {
            toast.parentNode.removeChild(toast);
        }
    }, 3400);
}
