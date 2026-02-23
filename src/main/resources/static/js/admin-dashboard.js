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

function showChartFallback(canvas, message) {
    if (!canvas || !canvas.parentElement) {
        return;
    }
    const fallback = document.createElement("div");
    fallback.className = "chart-fallback";
    fallback.textContent = message;
    canvas.parentElement.innerHTML = "";
    canvas.parentElement.appendChild(fallback);
}

function parseStringArray(value) {
    if (!Array.isArray(value)) {
        return [];
    }
    return value.map(item => String(item));
}

function parseNumberArray(value) {
    if (!Array.isArray(value)) {
        return [];
    }
    return value.map(item => Number(item) || 0);
}

function renderMonthlyComplaintChart(data) {
    const canvas = document.getElementById("monthlyComplaintChart");
    if (!canvas) {
        return;
    }
    if (typeof Chart === "undefined") {
        showChartFallback(canvas, "Unable to load monthly chart.");
        return;
    }

    const labels = parseStringArray(data.monthlyLabels);
    const counts = parseNumberArray(data.monthlyCounts);

    if (labels.length === 0 || counts.length === 0) {
        showChartFallback(canvas, "No monthly complaint data available.");
        return;
    }

    new Chart(canvas, {
        type: "line",
        data: {
            labels,
            datasets: [{
                label: "Complaints",
                data: counts,
                borderColor: "#1b4f72",
                backgroundColor: "rgba(27, 79, 114, 0.16)",
                fill: true,
                pointBackgroundColor: "#145374",
                pointBorderColor: "#145374",
                pointRadius: 3,
                tension: 0.25
            }]
        },
        options: {
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        precision: 0
                    },
                    grid: {
                        color: "rgba(0,0,0,0.08)"
                    }
                },
                x: {
                    grid: {
                        display: false
                    }
                }
            }
        }
    });
}

function renderComplaintTypePieChart(data) {
    const canvas = document.getElementById("complaintTypePieChart");
    if (!canvas) {
        return;
    }
    if (typeof Chart === "undefined") {
        showChartFallback(canvas, "Unable to load complaint type chart.");
        return;
    }

    const rawLabels = parseStringArray(data.complaintTypeLabels);
    const rawCounts = parseNumberArray(data.complaintTypeCounts);

    const labels = [];
    const counts = [];
    for (let index = 0; index < rawLabels.length; index++) {
        if ((rawCounts[index] || 0) > 0) {
            labels.push(rawLabels[index]);
            counts.push(rawCounts[index]);
        }
    }

    if (labels.length === 0 || counts.length === 0) {
        showChartFallback(canvas, "No complaint type data available.");
        return;
    }

    const palette = [
        "#145374",
        "#2f6f8f",
        "#5588a3",
        "#7aa5bf",
        "#a3bed4",
        "#bc6c25",
        "#dda15e",
        "#8c3f2d"
    ];

    new Chart(canvas, {
        type: "pie",
        data: {
            labels,
            datasets: [{
                data: counts,
                backgroundColor: labels.map((_, index) => palette[index % palette.length]),
                borderColor: "#ffffff",
                borderWidth: 2
            }]
        },
        options: {
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: "bottom"
                }
            }
        }
    });
}

window.addEventListener("load", () => {
    const analyticsData = window.adminAnalyticsData || {};
    renderMonthlyComplaintChart(analyticsData);
    renderComplaintTypePieChart(analyticsData);
});
