document.addEventListener('DOMContentLoaded', function() {
    // Global variables
    var charts = {};
    var currentPage = 1;
    var pageSize = 50;
    var totalPages = 1;
    var totalElements = 0;
    var currentSearch = '';
    var currentStatus = 'all';
    var currentSortBy = 'startTime';
    var currentSortOrder = 'desc';
    var isLoading = false;

    // API Configuration
    var API_BASE_URL = '/api';
    var ENDPOINTS = {
        TRANSACTIONS: API_BASE_URL + '/tx-logs',
        METRICS: API_BASE_URL + '/tx-metrics',
        CHARTS: API_BASE_URL + '/tx-charts'
    };

    // Initialize data
    function initializeData() {
        currentPage = 1;
        loadMetrics();
        loadChartData();
        loadTransactions();
    }

    // Load metrics from server
    function loadMetrics() {
        var xhr = new XMLHttpRequest();
        xhr.open('GET', ENDPOINTS.METRICS, true);
        xhr.onreadystatechange = function() {
            if (xhr.readyState === 4) {
                if (xhr.status === 200) {
                    var metrics = JSON.parse(xhr.responseText);
                    updateMetricsDisplay(metrics);
                    updateStatusChart(metrics.committedCount, metrics.rolledBackCount);
                } else {
                    console.error('Error loading metrics:', xhr.statusText);
                    // Fallback to mock data for demo
                    updateMetricsDisplay({
                        totalTransactions: 0,
                        successRate: 0.0,
                        committedCount: 0,
                        rolledBackCount: 0,
                        avgDuration: 0.0
                    });
                    updateStatusChart(0, 0);
                }
            }
        };
        xhr.send();
    }

    // Load chart data from server
    function loadChartData() {
        var xhr = new XMLHttpRequest();
        xhr.open('GET', ENDPOINTS.CHARTS, true);
        xhr.onreadystatechange = function() {
            if (xhr.readyState === 4) {
                if (xhr.status === 200) {
                    var chartData = JSON.parse(xhr.responseText);
                    updateCharts(chartData);
                } else {
                    console.error('Error loading chart data:', xhr.statusText);
                    // Fallback to mock data for demo
                    updateCharts(generateMockChartData());
                }
            }
        };
        xhr.send();
    }

    // Load transactions with server-side pagination, search, and sort
    function loadTransactions() {
        if (isLoading) return;

        isLoading = true;
        showTableLoading(true);

        // Build query parameters
        var params = [];
        params.push('page=' + (currentPage - 1)); // Spring Boot pages are 0-indexed
        params.push('size=' + pageSize);
        params.push('sort=' + currentSortBy + ',' + currentSortOrder);

        if (currentSearch) {
            params.push('search=' + encodeURIComponent(currentSearch));
        }
        if (currentStatus !== 'all') {
            params.push('status=' + currentStatus);
        }

        var url = ENDPOINTS.TRANSACTIONS + '?' + params.join('&');

        var xhr = new XMLHttpRequest();
        xhr.open('GET', url, true);
        xhr.onreadystatechange = function() {
            if (xhr.readyState === 4) {
                isLoading = false;
                showTableLoading(false);

                if (xhr.status === 200) {
                    var data = JSON.parse(xhr.responseText);
                    updateTransactionTable(data);
                    updatePaginationInfo(data);
                    updatePaginationControls(data);
                } else {
                    console.error('Error loading transactions:', xhr.statusText);
                    // Fallback to mock data for demo
                }
            }
        };
        xhr.send();
    }

    // Update metrics display
    function updateMetricsDisplay(metrics) {
        document.getElementById('totalTransactions').textContent = metrics.totalTransactions.toLocaleString();
        document.getElementById('successRate').textContent = metrics.successRate.toFixed(1) + '%';
        document.getElementById('committedCount').textContent = metrics.committedCount.toLocaleString();
        document.getElementById('rolledBackCount').textContent = metrics.rolledBackCount.toLocaleString();
        document.getElementById('avgDuration').textContent = formatDuration(metrics.avgDuration);
    }

    // Update transaction table
    function updateTransactionTable(data) {
        var tbody = document.getElementById('transactionTableBody');
        tbody.innerHTML = '';

        for (var i = 0; i < data.content.length; i++) {
            var transaction = data.content[i];

            var statusBadge = transaction.status === 'COMMITTED'
                ? '<span class="badge success"><i class="fas fa-check-circle"></i> Committed</span>'
                : '<span class="badge error"><i class="fas fa-times-circle"></i> Rolled Back</span>';

            var durationBadge = transaction.alarming
                ? '<span class="badge warning">' + formatDuration(transaction.duration) + '</span>'
                : '<span class="badge secondary">' + formatDuration(transaction.duration) + '</span>';

            var row = document.createElement('tr');
            row.innerHTML =
                '<td><span class="transaction-id">TX-' + String(transaction.txId).padStart(5, '0') + '</span></td>' +
                '<td><span class="method-name">' + transaction.method + '</span></td>' +
                '<td>' + formatTime(transaction.startTime) + '</td>' +
                '<td>' + formatTime(transaction.endTime) + '</td>' +
                '<td>' + durationBadge + '</td>' +
                '<td>' + statusBadge + '</td>' +
                '<td><span class="thread-id">' + transaction.thread + '</span></td>';

            tbody.appendChild(row);
        }
    }

    // Update pagination info
    function updatePaginationInfo(data) {
        var startRecord = data.totalElements === 0 ? 0 : data.number * data.size + 1;
        var endRecord = Math.min((data.number + 1) * data.size, data.totalElements);

        document.getElementById('startRecord').textContent = startRecord.toLocaleString();
        document.getElementById('endRecord').textContent = endRecord.toLocaleString();
        document.getElementById('totalResults').textContent = data.totalElements.toLocaleString();

        document.getElementById('tableFooter').style.display = 'flex';
    }

    // Update pagination controls
    function updatePaginationControls(data) {
        totalPages = data.totalPages;
        totalElements = data.totalElements;
        currentPage = data.number + 1; // Convert from 0-indexed to 1-indexed

        // Update page jump input
        var pageJump = document.getElementById('pageJump');
        pageJump.setAttribute('max', totalPages);
        pageJump.value = currentPage;

        // Update navigation buttons
        document.getElementById('firstPage').disabled = data.first;
        document.getElementById('prevPage').disabled = data.first;
        document.getElementById('nextPage').disabled = data.last;
        document.getElementById('lastPage').disabled = data.last;

        // Generate page numbers
        generatePageNumbers();
    }

    // Generate page number buttons
    function generatePageNumbers() {
        var pageNumbers = document.getElementById('pageNumbers');
        pageNumbers.innerHTML = '';

        if (totalPages <= 1) return;

        var maxVisiblePages = 5;
        var startPage = Math.max(1, currentPage - Math.floor(maxVisiblePages / 2));
        var endPage = Math.min(totalPages, startPage + maxVisiblePages - 1);

        // Adjust start page if we're near the end
        if (endPage - startPage < maxVisiblePages - 1) {
            startPage = Math.max(1, endPage - maxVisiblePages + 1);
        }

        // Add ellipsis and first page if needed
        if (startPage > 1) {
            var firstBtn = document.createElement('button');
            firstBtn.className = 'pagination-btn page-number';
            firstBtn.setAttribute('data-page', '1');
            firstBtn.textContent = '1';
            pageNumbers.appendChild(firstBtn);

            if (startPage > 2) {
                var ellipsis = document.createElement('span');
                ellipsis.className = 'pagination-ellipsis';
                ellipsis.textContent = '...';
                pageNumbers.appendChild(ellipsis);
            }
        }

        // Add page numbers
        for (var i = startPage; i <= endPage; i++) {
            var btn = document.createElement('button');
            btn.className = 'pagination-btn page-number' + (i === currentPage ? ' active' : '');
            btn.setAttribute('data-page', i);
            btn.textContent = i;
            pageNumbers.appendChild(btn);
        }

        // Add ellipsis and last page if needed
        if (endPage < totalPages) {
            if (endPage < totalPages - 1) {
                var ellipsis = document.createElement('span');
                ellipsis.className = 'pagination-ellipsis';
                ellipsis.textContent = '...';
                pageNumbers.appendChild(ellipsis);
            }

            var lastBtn = document.createElement('button');
            lastBtn.className = 'pagination-btn page-number';
            lastBtn.setAttribute('data-page', totalPages);
            lastBtn.textContent = totalPages;
            pageNumbers.appendChild(lastBtn);
        }
    }

    // Show/hide table loading state
    function showTableLoading(show) {
        var tableContainer = document.querySelector('.table-container');
        if (show) {
            tableContainer.classList.add('table-loading');
        } else {
            tableContainer.classList.remove('table-loading');
        }
    }

    // Go to specific page
    function goToPage(page) {
        if (page >= 1 && page <= totalPages && page !== currentPage && !isLoading) {
            currentPage = page;
            loadTransactions();
        }
    }

    // Simple debounce function
    function debounce(func, wait) {
        var timeout;
        return function() {
            var context = this;
            var args = arguments;
            var later = function() {
                timeout = null;
                func.apply(context, args);
            };
            clearTimeout(timeout);
            timeout = setTimeout(later, wait);
        };
    }

    // Handle search input with debouncing
    var debouncedSearch = debounce(function() {
        currentPage = 1;
        loadTransactions();
    }, 500);

    // Handle filter/sort changes
    function handleFilterSortChange() {
        currentPage = 1;
        loadTransactions();
    }

    // Format duration
    function formatDuration(ms) {
        if (ms < 1000) return Math.round(ms) + 'ms';
        return (ms / 1000).toFixed(2) + 's';
    }

    // Format time
    function formatTime(isoString) {
        return new Date(isoString).toLocaleString();
    }

    // Generate mock data for fallback (when API is not available)
    /*
    function generateMockTransactionData() {
        var mockTransactions = [];
        var statuses = ['COMMITTED', 'ROLLED_BACK'];
        var methods = [
            'UserService.createUser',
            'OrderService.processOrder',
            'PaymentService.processPayment',
            'InventoryService.updateStock',
            'NotificationService.sendEmail'
        ];

        for (var i = 0; i < pageSize; i++) {
            var startTime = new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000);
            var duration = Math.random() * 5000 + 50;
            var endTime = new Date(startTime.getTime() + duration);
            var status = Math.random() > 0.15 ? 'COMMITTED' : 'ROLLED_BACK';

            mockTransactions.push({
                txId: 'tx-' + String((currentPage - 1) * pageSize + i + 1).padStart(4, '0'),
                method: methods[Math.floor(Math.random() * methods.length)],
                startTime: startTime.toISOString(),
                endTime: endTime.toISOString(),
                duration: Math.round(duration),
                status: status,
                thread: 'thread-' + (Math.floor(Math.random() * 20) + 1),
                isolationLevel: 'READ_COMMITTED',
                propagation: 'REQUIRED'
            });
        }

        return {
            content: mockTransactions,
            totalElements: 1250,
            totalPages: Math.ceil(1250 / pageSize),
            number: currentPage - 1,
            size: pageSize,
            first: currentPage === 1,
            last: currentPage === Math.ceil(1250 / pageSize)
        };
    }
    */

    // Generate mock chart data
    function generateMockChartData() {
        var hourlyData = [];
        for (var hour = 0; hour < 24; hour++) {
            hourlyData.push({
                hour: hour + ':00',
                total: Math.floor(Math.random() * 100) + 20,
                successRate: Math.random() * 20 + 80
            });
        }

        return {
            statusDistribution: {
                committed: 0,
                rolledBack: 0
            },
            durationDistribution: [
                { range: '0-100ms', count: 0 },
                { range: '100-500ms', count: 0 },
                { range: '500ms-1s', count: 0 },
                { range: '1-2s', count: 0 },
                { range: '2-5s', count: 0 },
                { range: '5s+', count: 0 }
            ],
            hourlyData: hourlyData
        };
    }

    // Update charts with server data
    function updateCharts(chartData) {
        // updateStatusChart(chartData.statusDistribution);
        updateDurationChart(chartData.durationDistribution);
        // updateHourlyChart(chartData.hourlyData);
        // updateTrendsChart(chartData.hourlyData);
    }

    // Status distribution pie chart
    function updateStatusChart(committed, rolledBack) {
        var ctx = document.getElementById('statusChart').getContext('2d');

        if (charts.statusChart) {
            charts.statusChart.destroy();
        }

        charts.statusChart = new Chart(ctx, {
            type: 'pie',
            data: {
                labels: ['Committed', 'Rolled Back'],
                datasets: [{
                    data: [committed, rolledBack],
                    backgroundColor: ['#22c55e', '#ef4444'],
                    borderWidth: 2,
                    borderColor: '#ffffff'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        position: 'bottom',
                        labels: {
                            padding: 20,
                            usePointStyle: true
                        }
                    },
                    tooltip: {
                        callbacks: {
                            label: function(context) {
                                var total = context.dataset.data.reduce(function(a, b) { return a + b; }, 0);
                                var percentage = ((context.parsed / total) * 100).toFixed(1);
                                return context.label + ': ' + context.parsed + ' (' + percentage + '%)';
                            }
                        }
                    }
                }
            }
        });
    }

    // Duration distribution bar chart
    function updateDurationChart(durationData) {
        var ctx = document.getElementById('durationChart').getContext('2d');

        if (charts.durationChart) {
            charts.durationChart.destroy();
        }

        var labels = [];
        var data = [];
        for (var i = 0; i < durationData.length; i++) {
            var range = durationData[i].range;
            labels.push(range.minMillis + "-" + range.maxMillis + "ms");
            data.push(durationData[i].count);
        }

        charts.durationChart = new Chart(ctx, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Transaction Count',
                    data: data,
                    backgroundColor: '#3b82f6',
                    borderColor: '#2563eb',
                    borderWidth: 1
                }]
            },
            options: {
                responsive: true,
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
                            stepSize: 1
                        }
                    }
                }
            }
        });
    }

    // Hourly transaction volume chart
    function updateHourlyChart(hourlyData) {
        var ctx = document.getElementById('hourlyChart').getContext('2d');

        if (charts.hourlyChart) {
            charts.hourlyChart.destroy();
        }

        var labels = [];
        var totalData = [];
        var successRateData = [];

        for (var i = 0; i < hourlyData.length; i++) {
            labels.push(hourlyData[i].hour);
            totalData.push(hourlyData[i].total);
            successRateData.push(hourlyData[i].successRate);
        }

        charts.hourlyChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [
                    {
                        label: 'Total Transactions',
                        data: totalData,
                        backgroundColor: 'rgba(59, 130, 246, 0.2)',
                        borderColor: '#3b82f6',
                        borderWidth: 2,
                        fill: true,
                        yAxisID: 'y'
                    },
                    {
                        label: 'Success Rate (%)',
                        data: successRateData,
                        backgroundColor: 'rgba(34, 197, 94, 0.2)',
                        borderColor: '#22c55e',
                        borderWidth: 2,
                        fill: false,
                        yAxisID: 'y1'
                    }
                ]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: {
                    mode: 'index',
                    intersect: false
                },
                scales: {
                    x: {
                        display: true,
                        title: {
                            display: true,
                            text: 'Hour'
                        }
                    },
                    y: {
                        type: 'linear',
                        display: true,
                        position: 'left',
                        title: {
                            display: true,
                            text: 'Transaction Count'
                        }
                    },
                    y1: {
                        type: 'linear',
                        display: true,
                        position: 'right',
                        title: {
                            display: true,
                            text: 'Success Rate (%)'
                        },
                        grid: {
                            drawOnChartArea: false
                        },
                        min: 0,
                        max: 100
                    }
                }
            }
        });
    }

    // Success rate trends chart
    function updateTrendsChart(hourlyData) {
        var ctx = document.getElementById('trendsChart').getContext('2d');

        if (charts.trendsChart) {
            charts.trendsChart.destroy();
        }

        var labels = [];
        var successRateData = [];

        for (var i = 0; i < hourlyData.length; i++) {
            labels.push(hourlyData[i].hour);
            successRateData.push(hourlyData[i].successRate);
        }

        charts.trendsChart = new Chart(ctx, {
            type: 'line',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Success Rate (%)',
                    data: successRateData,
                    backgroundColor: 'rgba(34, 197, 94, 0.1)',
                    borderColor: '#22c55e',
                    borderWidth: 3,
                    fill: true,
                    tension: 0.4
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                plugins: {
                    legend: {
                        display: false
                    }
                },
                scales: {
                    y: {
                        beginAtZero: true,
                        max: 100,
                        title: {
                            display: true,
                            text: 'Success Rate (%)'
                        }
                    },
                    x: {
                        title: {
                            display: true,
                            text: 'Hour'
                        }
                    }
                }
            }
        });
    }

    // Event listeners
    var tabButtons = document.querySelectorAll('.tab-btn');
    for (var i = 0; i < tabButtons.length; i++) {
        tabButtons[i].addEventListener('click', function() {
            var tabId = this.getAttribute('data-tab');

            var allTabButtons = document.querySelectorAll('.tab-btn');
            for (var j = 0; j < allTabButtons.length; j++) {
                allTabButtons[j].classList.remove('active');
            }
            this.classList.add('active');

            var allTabContents = document.querySelectorAll('.tab-content');
            for (var k = 0; k < allTabContents.length; k++) {
                allTabContents[k].classList.remove('active');
            }
            document.getElementById(tabId).classList.add('active');
        });
    }

    // Search input with debouncing
    document.getElementById('searchInput').addEventListener('input', function() {
        currentSearch = this.value;
        debouncedSearch();
    });

    // Filter and sort event listeners
    document.getElementById('statusFilter').addEventListener('change', function() {
        currentStatus = this.value;
        handleFilterSortChange();
    });

    document.getElementById('sortBy').addEventListener('change', function() {
        currentSortBy = this.value;
        handleFilterSortChange();
    });

    document.getElementById('sortOrder').addEventListener('click', function() {
        var currentOrder = this.getAttribute('data-order');
        var newOrder = currentOrder === 'asc' ? 'desc' : 'asc';

        this.setAttribute('data-order', newOrder);
        this.innerHTML = '<i class="fas fa-sort-amount-' + (newOrder === 'asc' ? 'up' : 'down') + '"></i> ' + (newOrder === 'asc' ? 'Asc' : 'Desc');

        currentSortOrder = newOrder;
        handleFilterSortChange();
    });

    // Pagination event listeners
    document.getElementById('pageSize').addEventListener('change', function() {
        pageSize = parseInt(this.value);
        currentPage = 1;
        loadTransactions();
    });

    document.getElementById('firstPage').addEventListener('click', function() {
        goToPage(1);
    });

    document.getElementById('prevPage').addEventListener('click', function() {
        goToPage(currentPage - 1);
    });

    document.getElementById('nextPage').addEventListener('click', function() {
        goToPage(currentPage + 1);
    });

    document.getElementById('lastPage').addEventListener('click', function() {
        goToPage(totalPages);
    });

    document.addEventListener('click', function(e) {
        if (e.target.classList.contains('page-number')) {
            var page = parseInt(e.target.getAttribute('data-page'));
            goToPage(page);
        }
    });

    document.getElementById('jumpToPage').addEventListener('click', function() {
        var page = parseInt(document.getElementById('pageJump').value);
        goToPage(page);
    });

    document.getElementById('pageJump').addEventListener('keypress', function(e) {
        if (e.key === 'Enter' || e.keyCode === 13) {
            var page = parseInt(this.value);
            goToPage(page);
        }
    });

    document.getElementById('refreshBtn').addEventListener('click', function() {
        this.classList.add('loading');

        setTimeout(function() {
            initializeData();
            document.getElementById('refreshBtn').classList.remove('loading');
        }, 500);
    });

    // Initialize the dashboard
    initializeData();
});