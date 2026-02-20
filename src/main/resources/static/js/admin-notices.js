function togglePreviousNotices(listId, buttonElement) {
    const noticeList = document.getElementById(listId);
    if (!noticeList) {
        return;
    }

    const olderItems = noticeList.querySelectorAll(".notice-old");
    if (olderItems.length === 0) {
        return;
    }

    const hasHiddenItems = Array.from(olderItems).some(function (item) {
        return item.classList.contains("notice-hidden");
    });

    olderItems.forEach(function (item) {
        if (hasHiddenItems) {
            item.classList.remove("notice-hidden");
        } else {
            item.classList.add("notice-hidden");
        }
    });

    buttonElement.textContent = hasHiddenItems ? "Hide Previous Notices" : "Show Previous Notices";
}
