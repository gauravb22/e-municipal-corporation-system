function openPopup(){
    document.getElementById("popup").style.display="flex";
}
function selectLang(l){
    localStorage.setItem("language",l);
    location.href="/dashboard";
}
