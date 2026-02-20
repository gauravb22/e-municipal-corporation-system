const sidebar = document.getElementById("sidebar");
const overlay = document.getElementById("overlay");

function toggleMenu() {
    sidebar.classList.toggle("open");
    overlay.classList.toggle("show");
}

function closeMenu() {
    sidebar.classList.remove("open");
    overlay.classList.remove("show");
}
