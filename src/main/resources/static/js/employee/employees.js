let currentUrl = '/ui-api/employees?page=0&size=10&sort=fname,asc';
let prevUrl = null;
let nextUrl = null;
let debounceTimer; // Timer for instant search

document.addEventListener("DOMContentLoaded", () => {
    fetchEmployees(currentUrl);

    // Dropdown change triggers search with new size
    document.getElementById('sizeSelect').addEventListener('change', handleSearch);

    // INSTANT SEARCH: Listen to 'input' events (triggers as you type)
    document.getElementById('searchFname').addEventListener('input', handleSearch);
    document.getElementById('searchLname').addEventListener('input', handleSearch);
    document.getElementById('searchJobLvl').addEventListener('input', handleSearch);

    // Clear Filters
    document.getElementById('clearBtn').addEventListener('click', () => {
        document.getElementById('searchFname').value = '';
        document.getElementById('searchLname').value = '';
        document.getElementById('searchJobLvl').value = '';
        document.getElementById('sizeSelect').value = '10';
        fetchEmployees('/ui-api/employees?page=0&size=10&sort=fname,asc');
    });

    // Pagination
    document.getElementById('prevBtn').addEventListener('click', () => {
        if (prevUrl) fetchEmployees(prevUrl);
    });
    document.getElementById('nextBtn').addEventListener('click', () => {
        if (nextUrl) fetchEmployees(nextUrl);
    });
});

// The Debounce Search Function
function handleSearch() {
    clearTimeout(debounceTimer); // Cancel the previous timer if user is still typing

    // Wait 300ms after typing stops to hit the backend
    debounceTimer = setTimeout(() => {
        let fname = document.getElementById('searchFname').value.trim();
        let lname = document.getElementById('searchLname').value.trim();
        let jobLvl = document.getElementById('searchJobLvl').value.trim();
        let size = document.getElementById('sizeSelect').value;

        // If all search boxes are empty, go back to the standard master list
        if (fname === "" && lname === "" && jobLvl === "") {
            fetchEmployees(`/ui-api/employees?page=0&size=${size}&sort=fname,asc`);
            return;
        }

        // Otherwise, build the advanced search URL dynamically
        let query = `page=0&size=${size}&sort=fname,asc`;
        if (fname !== "") query += `&fname=${encodeURIComponent(fname)}`;
        if (lname !== "") query += `&lname=${encodeURIComponent(lname)}`;
        if (jobLvl !== "") query += `&jobLvl=${encodeURIComponent(jobLvl)}`;

        fetchEmployees(`/ui-api/employees/search/advanced?${query}`);
    }, 300);
}

// ... KEEP YOUR EXISTING fetchEmployees, renderTable, updatePagination, viewDetails, and deleteEmployee functions down here ...

function fetchEmployees(url) {
    fetch(url)
        .then(response => response.json())
        .then(data => {
            let employees = data._embedded ? data._embedded.employees : [];
            renderTable(employees);
            updatePagination(data);
        })
        .catch(error => console.error("Error fetching data:", error));
}

function renderTable(employees) {
    const tbody = document.getElementById('employeeTableBody');
    tbody.innerHTML = '';

    if (employees.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4">No employees found.</td></tr>';
        return;
    }

    employees.forEach(emp => {
        let row = `<tr>
            <td>${emp.fname}</td>
            <td>${emp.lname}</td>
            <td>${emp.jobLvl}</td>
            <td>
                <button onclick="viewDetails('${emp._links.self.href}')">View</button>
                <button onclick="deleteEmployee('${emp._links.self.href}')">Delete</button>
            </td>
        </tr>`;
        tbody.insertAdjacentHTML('beforeend', row);
    });
}

function updatePagination(data) {
    const prevBtn = document.getElementById('prevBtn');
    const nextBtn = document.getElementById('nextBtn');
    const pageInfo = document.getElementById('pageInfo');

    if (!data.page) {
        prevBtn.disabled = true;
        nextBtn.disabled = true;
        pageInfo.innerText = "Search Results";
        return;
    }

    pageInfo.innerText = `Page ${data.page.number + 1} of ${data.page.totalPages} (Total: ${data.page.totalElements})`;

    if (data._links.prev) {
        prevUrl = data._links.prev.href.replace("http://localhost:8080/api", "/ui-api");
        prevBtn.disabled = false;
    } else {
        prevUrl = null;
        prevBtn.disabled = true;
    }

    if (data._links.next) {
        nextUrl = data._links.next.href.replace("http://localhost:8080/api", "/ui-api");
        nextBtn.disabled = false;
    } else {
        nextUrl = null;
        nextBtn.disabled = true;
    }
}

function viewDetails(url) {
    console.log("View clicked for:", url);
}

function deleteEmployee(url) {
    console.log("Delete clicked for:", url);
}