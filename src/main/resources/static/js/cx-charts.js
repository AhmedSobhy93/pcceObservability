export function emptyChart(canvas, message = "No data") {
    if (!canvas) {
        return;
    }
    const context = canvas.getContext("2d");
    const width = canvas.clientWidth || canvas.width || 320;
    const height = canvas.clientHeight || canvas.height || 180;
    context.clearRect(0, 0, width, height);
    context.fillStyle = "#87a1c2";
    context.font = "14px Segoe UI, Arial, sans-serif";
    context.textAlign = "center";
    context.fillText(message, width / 2, height / 2);
}
