(function () {
    function num(value) {
        if (value === null || value === undefined || Number.isNaN(Number(value))) {
            return 0;
        }
        return Number(value);
    }

    function setupCanvas(canvas) {
        const ratio = window.devicePixelRatio || 1;
        const rect = canvas.getBoundingClientRect();
        canvas.width = rect.width * ratio;
        canvas.height = rect.height * ratio;
        const ctx = canvas.getContext("2d");
        ctx.setTransform(ratio, 0, 0, ratio, 0, 0);
        return ctx;
    }

    function drawGrid(ctx, width, height, pad) {
        ctx.strokeStyle = "#263241";
        ctx.lineWidth = 1;
        ctx.setLineDash([5, 5]);
        for (let i = 0; i <= 4; i++) {
            const y = pad.top + i * (height - pad.top - pad.bottom) / 4;
            ctx.beginPath();
            ctx.moveTo(pad.left, y);
            ctx.lineTo(width - pad.right, y);
            ctx.stroke();
        }
        ctx.setLineDash([]);
    }

    function drawLabels(ctx, labels, width, height, pad) {
        ctx.fillStyle = "#87a1c2";
        ctx.font = "14px Segoe UI";
        labels.forEach((label, index) => {
            const x = pad.left + (labels.length <= 1 ? 0 : index * (width - pad.left - pad.right) / (labels.length - 1));
            ctx.fillText(label, x - 18, height - 16);
        });
    }

    function drawLineChart(canvas, labels, series, fixedMax) {
        if (!canvas) return;
        const ctx = setupCanvas(canvas);
        const { width, height } = canvas.getBoundingClientRect();
        const pad = { left: 58, right: 24, top: 24, bottom: 44 };
        ctx.clearRect(0, 0, width, height);
        drawGrid(ctx, width, height, pad);
        const max = fixedMax || Math.max(10, ...series.flatMap(s => s.values));
        const pointsFor = values => values.map((value, index) => ({
            x: pad.left + (labels.length <= 1 ? 0 : index * (width - pad.left - pad.right) / (labels.length - 1)),
            y: height - pad.bottom - (num(value) / max) * (height - pad.top - pad.bottom)
        }));
        series.forEach(s => {
            const points = pointsFor(s.values);
            if (!points.length) return;
            ctx.strokeStyle = s.color;
            ctx.lineWidth = 3;
            ctx.beginPath();
            points.forEach((point, index) => index ? ctx.lineTo(point.x, point.y) : ctx.moveTo(point.x, point.y));
            ctx.stroke();
            ctx.lineTo(points[points.length - 1].x, height - pad.bottom);
            ctx.lineTo(points[0].x, height - pad.bottom);
            ctx.closePath();
            ctx.fillStyle = s.color + "24";
            ctx.fill();
        });
        drawLabels(ctx, labels, width, height, pad);
    }

    function drawBarChart(canvas, labels, values, color) {
        if (!canvas) return;
        const ctx = setupCanvas(canvas);
        const { width, height } = canvas.getBoundingClientRect();
        const pad = { left: 48, right: 20, top: 20, bottom: 42 };
        ctx.clearRect(0, 0, width, height);
        drawGrid(ctx, width, height, pad);
        const max = Math.max(10, ...values);
        const slot = (width - pad.left - pad.right) / Math.max(1, values.length);
        values.forEach((value, index) => {
            const barHeight = (num(value) / max) * (height - pad.top - pad.bottom);
            ctx.fillStyle = color;
            ctx.fillRect(pad.left + index * slot + slot * .2, height - pad.bottom - barHeight, slot * .6, barHeight);
        });
        drawLabels(ctx, labels, width, height, pad);
    }

    function roundRect(ctx, x, y, width, height, radius) {
        if (!Number.isFinite(width) || !Number.isFinite(height) || width <= 0 || height <= 0) {
            return;
        }
        const r = Math.max(0, Math.min(radius, width / 2, height / 2));
        ctx.beginPath();
        ctx.moveTo(x + r, y);
        ctx.arcTo(x + width, y, x + width, y + height, r);
        ctx.arcTo(x + width, y + height, x, y + height, r);
        ctx.arcTo(x, y + height, x, y, r);
        ctx.arcTo(x, y, x + width, y, r);
        ctx.closePath();
    }

    function drawHorizontalBarChart(canvas, labels, values, palette) {
        if (!canvas) return;
        const ctx = setupCanvas(canvas);
        const { width, height } = canvas.getBoundingClientRect();
        const pad = { left: 130, right: 38, top: 28, bottom: 34 };
        ctx.clearRect(0, 0, width, height);
        drawGrid(ctx, width, height, pad);
        const rows = labels.length || 1;
        const slot = (height - pad.top - pad.bottom) / rows;
        ctx.font = "14px Segoe UI";
        labels.forEach((label, index) => {
            const y = pad.top + index * slot + slot * .28;
            const barWidth = Math.max(0, Math.min(100, num(values[index]))) / 100 * (width - pad.left - pad.right);
            ctx.fillStyle = "#87a1c2";
            ctx.textAlign = "right";
            ctx.fillText(String(label).slice(0, 18), pad.left - 14, y + 10);
            ctx.fillStyle = palette[index % palette.length];
            roundRect(ctx, pad.left, y, barWidth, Math.max(16, slot * .38), 7);
            ctx.fill();
            ctx.textAlign = "left";
            ctx.fillStyle = "#f6f8fb";
            ctx.fillText(`${Math.round(num(values[index]))}%`, pad.left + barWidth + 8, y + 12);
        });
        ctx.textAlign = "left";
        ctx.fillStyle = "#87a1c2";
        [0, 25, 50, 75, 100].forEach(mark => {
            const x = pad.left + mark / 100 * (width - pad.left - pad.right);
            ctx.fillText(String(mark), x - 5, height - 10);
        });
    }

    function drawEmptyChart(canvas, message) {
        if (!canvas) return;
        const ctx = setupCanvas(canvas);
        const { width, height } = canvas.getBoundingClientRect();
        ctx.clearRect(0, 0, width, height);
        ctx.fillStyle = "#87a1c2";
        ctx.font = "16px Segoe UI";
        ctx.textAlign = "center";
        ctx.fillText(message, width / 2, height / 2);
        ctx.textAlign = "left";
    }

    function drawChartLegend(ctx, labels, palette, width, height) {
        if (!labels.length) return;
        ctx.font = "14px Segoe UI";
        let x = Math.max(16, width / 2 - labels.length * 56);
        const y = height - 18;
        labels.forEach((label, index) => {
            ctx.fillStyle = palette[index % palette.length];
            ctx.beginPath();
            ctx.arc(x, y - 4, 6, 0, Math.PI * 2);
            ctx.fill();
            ctx.fillStyle = "#87a1c2";
            ctx.fillText(String(label).slice(0, 14), x + 12, y);
            x += 110;
        });
    }

    function drawStackedBarChart(canvas, labels, series) {
        if (!canvas) return;
        const ctx = setupCanvas(canvas);
        const { width, height } = canvas.getBoundingClientRect();
        const pad = { left: 58, right: 24, top: 28, bottom: 54 };
        ctx.clearRect(0, 0, width, height);
        drawGrid(ctx, width, height, pad);
        const totals = labels.map((_, index) => series.reduce((total, item) => total + num(item.values[index]), 0));
        const max = Math.max(10, ...totals);
        const slot = (width - pad.left - pad.right) / Math.max(1, labels.length);
        labels.forEach((label, index) => {
            let yBase = height - pad.bottom;
            series.forEach(item => {
                const value = num(item.values[index]);
                const barHeight = value / max * (height - pad.top - pad.bottom);
                ctx.fillStyle = item.color;
                ctx.fillRect(pad.left + index * slot + slot * .18, yBase - barHeight, slot * .64, barHeight);
                yBase -= barHeight;
            });
        });
        drawLabels(ctx, labels, width, height, pad);
        drawChartLegend(ctx, series.map(item => item.label), series.map(item => item.color), width, height);
    }

    function drawDoughnut(canvas, labels, values, palette) {
        if (!canvas) return;
        const ctx = setupCanvas(canvas);
        const { width, height } = canvas.getBoundingClientRect();
        ctx.clearRect(0, 0, width, height);
        const total = values.reduce((sum, value) => sum + num(value), 0) || 1;
        const radius = Math.min(width, height) * .32;
        const cx = width / 2;
        const cy = height / 2;
        let start = -Math.PI / 2;
        values.forEach((value, index) => {
            const angle = (num(value) / total) * Math.PI * 2;
            ctx.beginPath();
            ctx.moveTo(cx, cy);
            ctx.fillStyle = palette[index % palette.length];
            ctx.arc(cx, cy, radius, start, start + angle);
            ctx.closePath();
            ctx.fill();
            start += angle;
        });
        ctx.beginPath();
        ctx.fillStyle = "#151a23";
        ctx.arc(cx, cy, radius * .58, 0, Math.PI * 2);
        ctx.fill();
    }

    function drawRadar(canvas, metrics, palette) {
        if (!canvas) return;
        const ctx = setupCanvas(canvas);
        const { width, height } = canvas.getBoundingClientRect();
        ctx.clearRect(0, 0, width, height);
        const colors = palette || ["#2ed3c2", "#3d82f6", "#f4a51c", "#8d6cf7", "#24e0a4", "#ff626c"];
        const series = Array.isArray(metrics) && metrics[0]?.metrics ? metrics : [{ label: "Metrics", color: "#2ed3c2", metrics }];
        const axes = series[0]?.metrics || [];
        const cx = width / 2;
        const cy = height / 2;
        const radius = Math.min(width, height) * .28;
        const count = axes.length || 1;
        ctx.strokeStyle = "#263241";
        ctx.fillStyle = "#87a1c2";
        ctx.font = "13px Segoe UI";
        for (let ring = 1; ring <= 4; ring++) {
            ctx.beginPath();
            for (let i = 0; i < count; i++) {
                const angle = -Math.PI / 2 + i * Math.PI * 2 / count;
                const r = radius * ring / 4;
                const x = cx + Math.cos(angle) * r;
                const y = cy + Math.sin(angle) * r;
                i ? ctx.lineTo(x, y) : ctx.moveTo(x, y);
            }
            ctx.closePath();
            ctx.stroke();
        }
        axes.forEach((metric, i) => {
            const angle = -Math.PI / 2 + i * Math.PI * 2 / count;
            ctx.beginPath();
            ctx.moveTo(cx, cy);
            ctx.lineTo(cx + Math.cos(angle) * radius, cy + Math.sin(angle) * radius);
            ctx.stroke();
            ctx.fillText(metric.label, cx + Math.cos(angle) * (radius + 18) - 28, cy + Math.sin(angle) * (radius + 18));
        });
        series.forEach((entry, seriesIndex) => {
            ctx.beginPath();
            entry.metrics.forEach((metric, i) => {
                const angle = -Math.PI / 2 + i * Math.PI * 2 / count;
                const value = Math.max(0, Math.min(100, num(metric.value))) / 100;
                const x = cx + Math.cos(angle) * radius * value;
                const y = cy + Math.sin(angle) * radius * value;
                i ? ctx.lineTo(x, y) : ctx.moveTo(x, y);
            });
            ctx.closePath();
            ctx.fillStyle = `${entry.color || colors[seriesIndex % colors.length]}33`;
            ctx.strokeStyle = entry.color || colors[seriesIndex % colors.length];
            ctx.lineWidth = 3;
            ctx.fill();
            ctx.stroke();
        });
        drawChartLegend(ctx, series.map(entry => entry.label), series.map((entry, index) => entry.color || colors[index % colors.length]), width, height);
    }

    window.CxDashboardCharts = {
        drawLineChart,
        drawBarChart,
        drawHorizontalBarChart,
        drawEmptyChart,
        drawStackedBarChart,
        drawDoughnut,
        drawRadar
    };
})();
