$(document).ready(() => {
    // Global variables
    let transactions = []
    let totalElements = 0
    let totalPages = 0
    let isFirstPage = false
    let isLastPage = false
    let currentPage = 1
    let pageSize = 10
    let sortField = "startTime"
    let sortDirection = "desc"
    const expandedRows = new Set()
    const timingColorClassMap = {
        "TRANSACTION_START": "start",
        "TRANSACTION_END": "end",
        "CONNECTION_ACQUIRED": "connection-acquired",
        "CONNECTION_RELEASED": "connection-released"
    }
    let charts = {}
    let alarmingThreshold = {
        transaction: 1000,
        connection: 1000
    }

    // API Configuration
    // Extract the current full pathname
    const pathname = window.location.pathname;

    // Find the context path by trimming '/tx-board/ui/index.html'
    const CONTEXT_PATH = pathname.replace("/tx-board/ui/index.html", "")
    const API_BASE_URL = CONTEXT_PATH + '/api/spring-tx-board'
    const ENDPOINTS = {
        ALARMING_THRESHOLD: API_BASE_URL + '/config/alarming-threshold',
        TRANSACTIONS: API_BASE_URL + '/tx-logs',
        SUMMARY: API_BASE_URL + '/tx-summary',
        CHARTS: API_BASE_URL + '/tx-charts'
    };

    // Initialize the dashboard
    initializeDashboard()

    function initializeDashboard() {
        setupEventListeners()
        fetchAlarmingThreshold()
        fetchAndUpdateUI()
    }

    function fetchAndUpdateUI() {
        updateSummary()
        loadDurationChartData()
        loadTransactions()
    }

    // Setup event listeners
    function setupEventListeners() {
        // Refresh button
        $("#refreshBtn").click(function () {
            $(this).find("i").addClass("loading")
            setTimeout(() => {
                fetchAndUpdateUI()
                $(this).find("i").removeClass("loading")
            }, 1000)
        })

        // Export button
        $("#exportBtn").click(exportToCSV)

        // Clear filters
        $("#clearFilters").click(clearAllFilters)

        // Filter inputs
        $("#statusFilter, #propagationFilter, #isolationFilter, #connectionFilter").change(function () {
            loadTransactions();
        })

        document.getElementById("methodSearch")
            .addEventListener("input", debounce(loadTransactions, 500));

        // Page size change
        $("#pageSize").change(function () {
            pageSize = Number.parseInt($(this).val())
            currentPage = 1
            loadTransactions()
        })

        // Sorting
        $(".sortable").click(function () {
            const field = $(this).data("sort")
            if (sortField === field) {
                sortDirection = sortDirection === "asc" ? "desc" : "asc"
            } else {
                sortField = field
                sortDirection = "asc"
            }
            updateSortIcons()
            loadTransactions()
        })

        // Pagination
        $("#firstPage").click(() => goToPage(1))
        $("#prevPage").click(() => goToPage(currentPage - 1))
        $("#nextPage").click(() => goToPage(currentPage + 1))
        $("#lastPage").click(() => goToPage(totalPages))

        $("#jumpToPage").click(() => {
            const page = parseInt($('#pageJump').val())
            goToPage(page)
        })

        // Modal
        $("#closeModal").click(closeModal)
        $(window).click((e) => {
            if (e.target.id === "transactionModal") {
                closeModal()
            }
        })

        // Modal tabs
        $(".tab-btn").click(function () {
            const tabId = $(this).data("tab")
            $(".tab-btn").removeClass("active")
            $(this).addClass("active")
            $(".tab-content").removeClass("active")
            $("#" + tabId).addClass("active")
        })
    }

    // Status distribution pie chart
    function updateStatusChart(committed, rolledBack, errored) {
        let ctx = document.getElementById('statusChart').getContext('2d');

        if (charts.statusChart) {
            charts.statusChart.destroy();
        }

        charts.statusChart = new Chart(ctx, {
            type: 'pie',
            data: {
                labels: ['Committed', 'Rolled Back', 'Errored'],
                datasets: [{
                    data: [committed, rolledBack, errored],
                    backgroundColor: ['#22c55e', '#fd2c9b', '#ef2b2b'],
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
                            label: function (context) {
                                const total = context.dataset.data.reduce(function (a, b) {
                                    return a + b;
                                }, 0);
                                const percentage = ((context.parsed / total) * 100).toFixed(1);
                                return context.label + ': ' + context.parsed + ' (' + percentage + '%)';
                            }
                        }
                    }
                }
            }
        });
    }

    function fetchAlarmingThreshold() {
        $.ajax({
            url: ENDPOINTS.ALARMING_THRESHOLD,
            method: 'GET',
            success: function (response) {
                alarmingThreshold.transaction = response.transaction
                alarmingThreshold.connection = response.connection
            },
            error: function (error) {
                console.error('Error to load alarming threshold values', error);
            }
        });
    }

    function loadDurationChartData() {
        $.ajax({
            url: ENDPOINTS.CHARTS,
            method: 'GET',
            success: function (response) {
                updateDurationChart(response.durationDistribution)
            },
            error: function (error) {
                console.error('Error loading duration chart data', error);
            }
        });
    }

    // Duration distribution bar chart
    function updateDurationChart(durationData) {
        const ctx = document.getElementById('durationChart').getContext('2d');

        if (charts.durationChart) {
            charts.durationChart.destroy();
        }

        const labels = [];
        const data = [];
        for (let i = 0; i < durationData.length; i++) {
            const range = durationData[i].range;
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

    // Load transactions (replace with actual API call)
    function loadTransactions() {
        const url = buildTxLogFetchingRequestUrl()

        $.ajax({
            url: url,
            method: 'GET',
            success: function (response) {
                transactions = response.content
                totalElements = response.totalElements
                totalPages = response.totalPages
                isFirstPage = response.first
                isLastPage = response.last
                renderTable()
                updatePaginationInfo(response)
                updatePaginationControls(response)
            },
            error: function (error) {
                console.error('Error loading transactions:', error);
            }
        });
    }

    // Apply filters and sorting
    function buildTxLogFetchingRequestUrl() {
        const methodSearch = $("#methodSearch").val().toLowerCase()
        const statusFilter = $("#statusFilter").val()
        const propagationFilter = $("#propagationFilter").val()
        const isolationFilter = $("#isolationFilter").val()
        const connectionFilter = $("#connectionFilter").val()

        // Build query parameters
        const params = [];
        params.push('page=' + (currentPage - 1)); // Spring Boot pages are 0-indexed
        params.push('size=' + pageSize);

        if (statusFilter) {
            params.push('status=' + statusFilter)
        }

        if (propagationFilter) {
            params.push('propagation=' + propagationFilter)
        }

        if (isolationFilter) {
            params.push('isolation=' + isolationFilter)
        }

        if (connectionFilter) {
            params.push('connectionOriented=' + connectionFilter)
        }

        if (methodSearch) {
            params.push('search=' + encodeURIComponent(methodSearch))
        }

        if (sortField) {
            params.push('sort=' + sortField + ',' + sortDirection)
        }

        return ENDPOINTS.TRANSACTIONS + '?' + params.join('&');
    }

    // Render transaction table
    function renderTable() {
        const tbody = $("#transactionTableBody")
        tbody.empty()
        if (transactions.length === 0) {
            tbody.append(`
                <tr>
                    <td colspan="9" style="text-align: center; padding: 30px;">
                        No transactions found
                    </td>
                </tr>
            `)
            return
        }

        transactions.forEach((tx) => {
            renderTransaction(tx, tbody, 0)
        })
    }

    // Render a transaction row with its children
    function renderTransaction(tx, container, depth, parentId = null) {
        const txId = generateTransactionId(tx, parentId)
        const hasChildren = tx.child && tx.child.length > 0
        const isExpanded = expandedRows.has(txId)

        const row = $(`
            <tr class="${depth > 0 ? "child-row" : ""}" data-tx-id="${txId}" data-depth="${depth}">
                <td> ${hasChildren ? `<button class="expand-button ${isExpanded ? "expanded" : ""}" data-tx-id="${txId}">
                            <i class="fas fa-chevron-right"></i>
                        </button>` : ""}
                </td>
                <td>
                    ${generateIndentation(depth)}
                    <span class="method-name">${tx.method}</span>
                    ${generateMostParentTransactionId(tx)}
                </td>
                <td>${formatDateTime(tx.startTime)}</td>
                <td>
                    <span class="badge ${tx.alarmingTransaction ? "badge-warning" : "badge-secondary"}">
                        ${formatDuration(tx.duration)}
                    </span>
                </td>
                <td>${getStatusBadge(tx.status)}</td>
                <td><span class="badge badge-info">${tx.propagation}</span></td>
                <td><span class="badge badge-secondary">${tx.isolation}</span></td>
                <td class="thread-id">${tx.thread}</td>
                <td>
                    <span class="badge badge-info">${tx.executedQuires ? tx.executedQuires.length : 0}</span>
                </td>
                <td>
                    <button class="btn btn-sm btn-primary view-details" data-tx-id="${txId}" data-depth="${depth}">
                        <i class="fas fa-eye"></i> View
                    </button>
                </td>
            </tr>
        `)

        container.append(row)

        // Add event handlers
        row.find(".expand-button").click((e) => {
            e.stopPropagation()
            toggleExpand(txId)
        })

        row.find(".view-details").click(function (e) {
            e.stopPropagation()
            showTransactionDetails(tx, Number.parseInt($(this).data("depth")))
        })

        // Render children if expanded
        if (hasChildren && isExpanded) {
            tx.child.forEach((child) => {
                renderTransaction(child, container, depth + 1, txId)
            })
        }
    }

    // Generate a unique ID for a transaction
    function generateTransactionId(tx, parentId) {
        const baseId = `${tx.method}-${tx.startTime}`
        return parentId ? `${parentId}-${baseId}` : baseId
    }

    // Generate most parent tx-id
    function generateMostParentTransactionId(tx) {
        let tx_id = ""
        if (tx.txId) {
            const txId = `tx-${String(tx.txId).padStart(5, '0')}`
            tx_id += '<br><span class="tx-id">ID: ' + txId + '</span>'
        }

        return tx_id;
    }

    // Generate indentation for nested transactions
    function generateIndentation(depth) {
        let indentation = ""
        for (let i = 0; i < depth; i++) {
            indentation += '<span class="child-indent"></span>'
        }
        return indentation
    }

    // Toggle expand/collapse for a transaction
    function toggleExpand(txId) {
        if (expandedRows.has(txId)) {
            expandedRows.delete(txId)
            // Also remove any children that might be expanded
            const childPrefix = txId + "-"
            expandedRows.forEach((id) => {
                if (id.startsWith(childPrefix)) {
                    expandedRows.delete(id)
                }
            })
        } else {
            expandedRows.add(txId)
        }

        renderTable()
    }

    // Update pagination info
    function updatePaginationInfo(data) {
        const startRecord = data.totalElements === 0 ? 0 : data.page * data.size + 1
        const endRecord = Math.min((data.page + 1) * data.size, data.totalElements)

        $('#startRecord').text(startRecord.toLocaleString())
        $('#endRecord').text(endRecord.toLocaleString())
        $('#totalResults').text(data.totalElements.toLocaleString())

        $('#tableFooter').css('display', 'flex')
    }

    // Update pagination controls
    function updatePaginationControls(data) {
        totalPages = data.totalPages
        totalElements = data.totalElements
        currentPage = data.page + 1 // Convert from 0-indexed to 1-indexed

        // Update page jump input
        const pageJump = $('#pageJump');
        pageJump.attr('max', totalPages)
        pageJump.val(currentPage);

        // Update navigation buttons
        $('#firstPage, #prevPage').prop('disabled', data.first)
        $('#nextPage, #lastPage').prop('disabled', data.last)

        // Generate page numbers
        generatePageNumbers()
    }

    // Generate page number buttons
    function generatePageNumbers() {
        const pageNumbers = $('#pageNumbers')
        pageNumbers.empty()

        if (totalPages <= 1) return

        const maxVisiblePages = 5
        let startPage = Math.max(1, currentPage - Math.floor(maxVisiblePages / 2))
        let endPage = Math.min(totalPages, startPage + maxVisiblePages - 1)

        // Adjust start page if we're near the end
        if (endPage - startPage < maxVisiblePages - 1) {
            startPage = Math.max(1, endPage - maxVisiblePages + 1)
        }

        // Add ellipsis and first page if needed
        if (startPage > 1) {
            $('<button>')
                .addClass('pagination-btn page-number')
                .attr('data-page', '1')
                .text('1')
                .appendTo(pageNumbers)

            if (startPage > 2) {
                $('<span>')
                    .addClass('pagination-ellipsis')
                    .text('...')
                    .appendTo(pageNumbers)
            }
        }

        // Add page numbers
        for (let i = startPage; i <= endPage; i++) {
            $('<button>')
                .addClass('pagination-btn page-number' + (i === currentPage ? ' active' : ''))
                .attr('data-page', i)
                .text(i)
                .appendTo(pageNumbers)
        }

        // Add ellipsis and last page if needed
        if (endPage < totalPages) {
            if (endPage < totalPages - 1) {
                $('<span>')
                    .addClass('pagination-ellipsis')
                    .text('...')
                    .appendTo(pageNumbers)
            }

            $('<button>')
                .addClass('pagination-btn page-number')
                .attr('data-page', totalPages)
                .text(totalPages)
                .appendTo(pageNumbers)
        }

        // Attach click handler
        $('.page-number').off('click').on('click', function () {
            const page = parseInt($(this).attr('data-page'), 10)
            goToPage(page)
        })
    }

    // Go to specific page
    function goToPage(page) {
        if (page >= 1 && page <= totalPages && page !== currentPage) {
            currentPage = page
            loadTransactions()
        }
    }

    // Update summary cards
    function updateSummary() {
        $.ajax({
            url: ENDPOINTS.SUMMARY,
            method: 'GET',
            success: function (response) {
                showTransactionSummary(response)
                updateStatusChart(response.committedCount, response.rolledBackCount, response.erroredCount)
            },
            error: function (error) {
                console.error('Error loading summary:', error);
            }
        });
    }

    function showTransactionSummary(txSummary) {
        const totalTx = txSummary.totalTransaction
        const committedTx = txSummary.committedCount
        const successRate = (committedTx / totalTx) * 100
        const rolledBackTx = txSummary.rolledBackCount
        const erroredTx = txSummary.erroredCount

        let displaySuccessRate = "N/A"
        if (!isNaN(successRate)) {
            displaySuccessRate = `${successRate.toFixed(2)}%`
        }

        $("#totalTransactions").text(totalTx)
        $("#successRate").text(displaySuccessRate)
        $("#committedCount").text(committedTx)
        $("#rolledBackErroredCount").text(rolledBackTx + " / " + erroredTx)
        $("#avgDuration").text(formatDuration(txSummary.averageDuration))
        $("#avgConnOccupied").text(formatDuration(txSummary.averageConnectionOccupiedTime))
    }

    // Show transaction details modal
    function showTransactionDetails(tx, depth = 0) {
        // Overview tab
        $("#detailMethodName").text(tx.method)
        $("#detailStatus").removeClass().addClass("badge").addClass(getStatusClass(tx.status)).text(tx.status)
        $("#detailPropagation").text(tx.propagation)
        $("#detailIsolation").text(tx.isolation)
        $("#detailThread").text(tx.thread || "N/A")
        $("#detailDuration").text(formatDuration(tx.duration))
        $("#detailTotalTransactions").text(tx.totalTransactionCount || 1)
        $("#detailTotalQueries").text(tx.totalQueryCount || (tx.executedQuires ? tx.executedQuires.length : 0))

        // Connection summary tab
        const connSummaryTab = $("#connectionSummaryTab")
        if (depth === 0 && tx.connectionOriented) {
            connSummaryTab.show()
            // Build connection summary from events
            const summary = tx.connectionSummary
            $("#totalAcquiredConnection").text(summary.acquisitionCount)
            $("#alarmingConnection").text(summary.alarmingConnectionCount)
            $("#occupiedTime").text(formatDuration(summary.occupiedTime))
        } else {
            connSummaryTab.hide()
            // If connection summary tab was active, switch to overview
            if (connSummaryTab.hasClass("active")) {
                $(".tab-btn").removeClass("active")
                $(".tab-content").removeClass("active")
                $('[data-tab="overview"]').addClass("active")
                $("#overview").addClass("active")
            }
        }

        // Timing tab
        const timingTab = $("#timingTab")
        if (depth === 0) {
            timingTab.show()
            // Build timing from events
            buildTimingFromEvents(tx)
        } else {
            timingTab.hide()
            // If timing tab was active, switch to overview
            if (timingTab.hasClass("active")) {
                $(".tab-btn").removeClass("active")
                $(".tab-content").removeClass("active")
                $('[data-tab="overview"]').addClass("active")
                $("#overview").addClass("active")
            }
        }

        // SQL tab
        const queries = tx.executedQuires || []
        $("#sqlCount").text(`${queries.length} queries`)
        const sqlList = $("#sqlList")
        sqlList.empty()

        if (queries.length === 0) {
            sqlList.append('<p style="color: #64748b; text-align: center; padding: 20px;">No SQL queries executed</p>')
        } else {
            queries.forEach((sql, index) => {
                const sqlItem = `
                    <div class="sql-item">
                        <div class="sql-index">Query #${index + 1}</div>
                        <div class="sql-query">${sql}</div>
                    </div>
                `
                sqlList.append(sqlItem)
            })
        }

        // Children tab
        const children = tx.child || []
        $("#childrenCount").text(`${children.length} children`)
        const childrenTree = $("#childrenTree")
        childrenTree.empty()

        if (children.length === 0) {
            childrenTree.append('<p style="color: #64748b; text-align: center; padding: 20px;">No child transactions</p>')
        } else {
            children.forEach((child, index) => {
                const childItem = `
                    <div class="child-item">
                        <div class="child-method">${child.method}</div>
                        <div class="child-details">
                            <div class="detail">
                                <span class="label">Status:</span>
                                <span class="badge ${getStatusClass(child.status)}">${child.status}</span>
                            </div>
                            <div class="detail">
                                <span class="label">Duration:</span>
                                <span>${formatDuration(child.duration)}</span>
                            </div>
                            <div class="detail">
                                <span class="label">Propagation:</span>
                                <span>${child.propagation}</span>
                            </div>
                            <div class="detail">
                                <span class="label">SQL Queries:</span>
                                <span>${child.executedQuires ? child.executedQuires.length : 0}</span>
                            </div>
                        </div>
                    </div>
                `
                childrenTree.append(childItem)
            })
        }

        const postTxQuiresTab = $("#postTxQuiresTab")
        if (depth === 0) {
            postTxQuiresTab.show()
            // Build post transaction quires events
            const queries = tx.postTransactionQuires || []
            $("#postTxSqlCount").text(`${queries.length} queries`)
            const postTxSqlList = $("#postTxSqlList")
            postTxSqlList.empty()

            if (queries.length === 0) {
                postTxSqlList.append('<p style="color: #64748b; text-align: center; padding: 20px;">No post transaction SQL queries executed</p>')
            } else {
                queries.forEach((sql, index) => {
                    const sqlItem = `
                    <div class="sql-item">
                        <div class="sql-index">Query #${index + 1}</div>
                        <div class="sql-query">${sql}</div>
                    </div>
                `
                    postTxSqlList.append(sqlItem)
                })
            }
        } else {
            postTxQuiresTab.hide()
            // If post transaction quires tab was active, switch to overview
            if (postTxQuiresTab.hasClass("active")) {
                $(".tab-btn").removeClass("active")
                $(".tab-content").removeClass("active")
                $('[data-tab="overview"]').addClass("active")
                $("#overview").addClass("active")
            }
        }

        $("#transactionModal").show()
    }

    // Close modal
    function closeModal() {
        $("#transactionModal").hide()
    }

    // Clear all filters
    function clearAllFilters() {
        $("#methodSearch").val("")
        $("#statusFilter").val("")
        $("#propagationFilter").val("")
        $("#isolationFilter").val("")
        $("#connectionFilter").val("")
        loadTransactions()
    }

    // Export to CSV
    function exportToCSV() {
        const headers = [
            "Method Name",
            "Start Time",
            "End Time",
            "Duration",
            "Status",
            "Propagation",
            "Isolation",
            "SQL Count",
        ]
        const csvContent = [headers.join(",")]

        // Recursive function to add all transactions including children
        function addTransactionToCSV(tx, depth = 0) {
            const indent = depth > 0 ? " ".repeat(depth * 2) : ""
            const row = [
                `"${indent}${tx.method}"`,
                `"${formatDateTime(tx.startTime)}"`,
                `"${formatDateTime(tx.endTime)}"`,
                tx.duration,
                tx.status,
                tx.propagation,
                tx.isolation,
                tx.thread,
                tx.executedQuires ? tx.executedQuires.length : 0,
            ]
            csvContent.push(row.join(","))

            if (tx.child && tx.child.length > 0) {
                tx.child.forEach((child) => addTransactionToCSV(child, depth + 1))
            }
        }

        transactions.forEach((tx) => addTransactionToCSV(tx))

        const blob = new Blob([csvContent.join("\n")], {type: "text/csv"})
        const url = window.URL.createObjectURL(blob)
        const a = document.createElement("a")
        a.href = url
        a.download = `spring-transactions-${new Date().toISOString().split("T")[0]}.csv`
        a.click()
        window.URL.revokeObjectURL(url)
    }

    // Update sort icons
    function updateSortIcons() {
        $(".sortable").removeClass("asc desc")
        $(`.sortable[data-sort="${sortField}"]`).addClass(sortDirection)
    }

    // Utility functions
    function getStatusBadge(status) {
        const statusClass = getStatusClass(status)
        return `<span class="badge ${statusClass}">
                    <i class="fas fa-${status === 'COMMITTED' ? 'check' : 'times'}-circle"></i>${status}
                </span>`
    }

    function getStatusClass(status) {
        switch (status) {
            case "COMMITTED":
                return "badge-success"
            case "ROLLED_BACK":
                return "badge-error"
            case "ERRORED":
                return "badge-error"
            default:
                return "badge-secondary"
        }
    }

    function formatDuration(ms) {
        if (ms < 1000) return `${Math.round(ms)}ms`
        return `${(ms / 1000).toFixed(2)}s`
    }

    function formatDateTime(date) {
        return new Date(date).toLocaleString()
    }

    function debounce(func, delay) {
        let timeout;
        return function (...args) {
            clearTimeout(timeout);
            timeout = setTimeout(() => func.apply(this, args), delay);
        };
    }

    function buildTimingFromEvents(tx) {
        const events = tx.events || []
        const timeline = $(".timing-timeline")
        timeline.empty()

        if (events.length === 0) {
            timeline.append('<p style="color: #64748b; text-align: center; padding: 20px;">No timing events available</p>')
            return
        }

        const txTimelineStack = []
        const connTimelineStack = []
        events.forEach((event) => {
            let duration = undefined
            const timelinePointColorClass = timingColorClassMap[event.type];

            if (event.type === 'TRANSACTION_START') {
                txTimelineStack.push(event)
            } else if (event.type === 'TRANSACTION_END') {
                const prevEvent = txTimelineStack.pop()
                duration = calculateDuration(event, prevEvent)
            }

            if (event.type === 'CONNECTION_ACQUIRED') {
                connTimelineStack.push(event);
            } else if (event.type === 'CONNECTION_RELEASED') {
                const prevEvent = connTimelineStack.pop()
                duration = calculateDuration(event, prevEvent)
            }

            let isAlarming = false;
            if (duration !== undefined) {
                isAlarming = (event.type === 'TRANSACTION_END' && duration > alarmingThreshold.transaction) ||
                    (event.type === 'CONNECTION_RELEASED' && duration > alarmingThreshold.connection)
            }

            timeline.append(`
                <div class="timeline-item">
                    <div class="timeline-marker ${timelinePointColorClass}"></div>
                    <div class="timeline-content">
                        <h4>${event.details}</h4>
                        <p>${formatDateTime(event.timestamp)}</p>
                        ${duration === undefined ? '' : `
                            <small>
                                <span class="badge ${isAlarming ? 'badge-warning' : 'badge-secondary'}" >Duration: ${formatDuration(duration)}</span>
                            </small>
                        `}
                    </div>
                </div>
            `)
        })
    }

    function calculateDuration(currEvent, prevEvent) {
        return new Date(currEvent.timestamp).getTime() - new Date(prevEvent.timestamp).getTime()
    }
})
