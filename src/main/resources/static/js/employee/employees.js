// State variables
let currentUrl = '/ui-api/employees?page=0&size=10&sort=fname,asc';
let prevUrl = null;
let nextUrl = null;
let debounceTimer;
let jobDataMap = {};

const empIdRegex = /^([A-Z]{3}[1-9][0-9]{4}[FM]|[A-Z]-[A-Z][1-9][0-9]{4}[FM])$/;

document.addEventListener("DOMContentLoaded", () => {
    // PRE-LOAD DROPDOWNS: Fetch jobs and publishers immediately so they are ready for both Add and Edit modals
    populateDropdowns();
    fetchEmployees(currentUrl);

    // ==========================================
    // Master List Controls & Search
    // ==========================================
    document.getElementById('sizeSelect').addEventListener('change', handleSearch);
    document.getElementById('searchFname').addEventListener('input', handleSearch);
    document.getElementById('searchLname').addEventListener('input', handleSearch);
    document.getElementById('searchJobLvl').addEventListener('input', handleSearch);

    document.getElementById('clearBtn').addEventListener('click', () => {
        document.getElementById('searchFname').value = '';
        document.getElementById('searchLname').value = '';
        document.getElementById('searchJobLvl').value = '';
        document.getElementById('sizeSelect').value = '10';
        fetchEmployees('/ui-api/employees?page=0&size=10&sort=fname,asc');
    });

    document.getElementById('prevBtn').addEventListener('click', () => { if (prevUrl) fetchEmployees(prevUrl); });
    document.getElementById('nextBtn').addEventListener('click', () => { if (nextUrl) fetchEmployees(nextUrl); });

    // ==========================================
    // ADD Employee Logic
    // ==========================================
    const addModal = document.getElementById('addEmployeeModal');
    const addForm = document.getElementById('addEmployeeForm');

    document.getElementById('addEmployeeBtn').addEventListener('click', () => {
        addForm.reset();
        document.getElementById('empIdError').style.display = 'none';
        document.getElementById('jobLvlError').style.display = 'none';
        document.getElementById('formErrorMsg').style.display = 'none';
        document.getElementById('newEmpId').style.borderColor = '';
        document.getElementById('newJobLvl').style.borderColor = '';
        document.getElementById('jobLvlHelp').innerText = 'Select a job role to see allowed levels.';
        addModal.showModal();
    });

    document.getElementById('closeAddModalBtn').addEventListener('click', () => addModal.close());

    // Add Form - Dynamic Job Level Help
    document.getElementById('newJobSelect').addEventListener('change', (e) => {
        updateJobLevelHelp(e.target.value, document.getElementById('newJobLvl'), document.getElementById('jobLvlHelp'));
    });

    addForm.addEventListener('submit', (e) => {
        e.preventDefault();

        if (!validateJobLevel(document.getElementById('newJobSelect').value, document.getElementById('newJobLvl'), document.getElementById('jobLvlError'))) return;

        const empIdInput = document.getElementById('newEmpId');
        if (!empIdRegex.test(empIdInput.value.trim())) {
            empIdInput.style.borderColor = 'red';
            document.getElementById('empIdError').style.display = 'block';
            return;
        }

        const newEmployee = {
            empId: empIdInput.value.trim(),
            fname: document.getElementById('newFname').value.trim(),
            minit: document.getElementById('newMinit').value.trim() || null,
            lname: document.getElementById('newLname').value.trim(),
            jobId: parseInt(document.getElementById('newJobSelect').value, 10),
            jobLvl: parseInt(document.getElementById('newJobLvl').value, 10),
            pubId: document.getElementById('newPubSelect').value
        };

        // Send POST request to Proxy
        fetch('/ui-api/employees', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(newEmployee)
        }).then(async response => {
            if (response.ok || response.status === 201) {
                // Success!
                addModal.close();
                fetchEmployees(currentUrl);
            } else {
                // 1. Read the EXACT error text from the Java Server
                const errorText = await response.text();

                // 2. Bulletproof Check: If it's a 409 OR the error text mentions a duplicate
                if (response.status === 409 || errorText.toLowerCase().includes("already exists") || errorText.toLowerCase().includes("duplicate")) {
                    const empIdError = document.getElementById('empIdError');
                    empIdInput.style.borderColor = 'red';
                    empIdError.innerText = "This Employee ID is already taken in the database.";
                    empIdError.style.display = 'block';
                    document.getElementById('formErrorMsg').style.display = 'none';
                } else {
                    // 3. For any other error, print the actual server message to the screen
                    document.getElementById('formErrorMsg').innerHTML = `<strong>Server Error ${response.status}:</strong> ${errorText || 'The server rejected the data.'}`;
                    document.getElementById('formErrorMsg').style.display = 'block';
                }
            }
        }).catch(error => {
            console.error("Network error:", error);
            document.getElementById('formErrorMsg').innerText = "Network error occurred. Is the server running?";
            document.getElementById('formErrorMsg').style.display = 'block';
        });
    });

    // ==========================================
    // EDIT Employee Logic
    // ==========================================
    const editModal = document.getElementById('editEmployeeModal');
    const editForm = document.getElementById('editEmployeeForm');

    document.getElementById('closeEditModalBtn').addEventListener('click', () => editModal.close());

    // Edit Form - Dynamic Job Level Help
    document.getElementById('editJobSelect').addEventListener('change', (e) => {
        updateJobLevelHelp(e.target.value, document.getElementById('editJobLvl'), document.getElementById('editJobLvlHelp'));
    });

    editForm.addEventListener('submit', (e) => {
        e.preventDefault();

        if (!validateJobLevel(document.getElementById('editJobSelect').value, document.getElementById('editJobLvl'), document.getElementById('editJobLvlError'))) return;

        const empId = document.getElementById('editEmpId').value;
        const updates = {
            fname: document.getElementById('editFname').value.trim(),
            minit: document.getElementById('editMinit').value.trim() || null,
            lname: document.getElementById('editLname').value.trim(),
            jobId: parseInt(document.getElementById('editJobSelect').value, 10),
            jobLvl: parseInt(document.getElementById('editJobLvl').value, 10),
            pubId: document.getElementById('editPubSelect').value
        };

        // Send a PATCH request to update only the modified fields
        fetch(`/ui-api/employees/${empId}`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updates)
        }).then(async response => {
            if (response.ok || response.status === 200) {
                editModal.close();
                fetchEmployees(currentUrl);
            } else {
                // Read and display the exact error text for edits too
                const errorText = await response.text();
                document.getElementById('editFormErrorMsg').innerHTML = `<strong>Server Error ${response.status}:</strong> ${errorText}`;
                document.getElementById('editFormErrorMsg').style.display = 'block';
            }
        }).catch(error => {
            console.error("Network error:", error);
            document.getElementById('editFormErrorMsg').innerText = "Network error occurred.";
            document.getElementById('editFormErrorMsg').style.display = 'block';
        });
    });
});

// ==========================================
// Shared Helpers for Add & Edit Modals
// ==========================================
function updateJobLevelHelp(jobId, inputElement, helpTextElement) {
    if (jobId && jobDataMap[jobId]) {
        const min = jobDataMap[jobId].minLvl;
        const max = jobDataMap[jobId].maxLvl;
        inputElement.min = min;
        inputElement.max = max;
        helpTextElement.innerText = `Allowed range for this role: ${min} - ${max}`;
    } else {
        helpTextElement.innerText = 'Select a job role to see allowed levels.';
        inputElement.removeAttribute('min');
        inputElement.removeAttribute('max');
    }
}

function validateJobLevel(jobId, inputElement, errorElement) {
    if (jobId && jobDataMap[jobId]) {
        const enteredLvl = parseInt(inputElement.value, 10);
        const min = jobDataMap[jobId].minLvl;
        const max = jobDataMap[jobId].maxLvl;
        if (enteredLvl < min || enteredLvl > max) {
            inputElement.style.borderColor = 'red';
            errorElement.innerText = `Must be between ${min} and ${max}.`;
            errorElement.style.display = 'inline';
            return false;
        }
    }
    inputElement.style.borderColor = '';
    errorElement.style.display = 'none';
    return true;
}

// Fetch Reference Data and populate BOTH Add and Edit Dropdowns
function populateDropdowns() {
    fetch('/ui-api/employees/reference/jobs')
        .then(res => res.json())
        .then(data => {
            const addJob = document.getElementById('newJobSelect');
            const editJob = document.getElementById('editJobSelect');
            if (data._embedded && data._embedded.jobs) {
                data._embedded.jobs.forEach(job => {
                    const jobId = job._links.self.href.split('/').pop();
                    addJob.add(new Option(job.jobDesc, jobId));
                    editJob.add(new Option(job.jobDesc, jobId));
                    jobDataMap[jobId] = { minLvl: job.minLvl, maxLvl: job.maxLvl };
                });
            }
        });

    fetch('/ui-api/employees/reference/publishers')
        .then(res => res.json())
        .then(data => {
            const addPub = document.getElementById('newPubSelect');
            const editPub = document.getElementById('editPubSelect');
            if (data._embedded && data._embedded.publishers) {
                data._embedded.publishers.forEach(pub => {
                    const pubId = pub._links.self.href.split('/').pop();
                    addPub.add(new Option(pub.pubName, pubId));
                    editPub.add(new Option(pub.pubName, pubId));
                });
            }
        });
}

// ==========================================
// ACTION BUTTONS (Edit, View, Delete)
// ==========================================
window.editEmployee = function(url) {
    const cleanUrl = url.split('{')[0];
    const id = cleanUrl.split('/').pop();

    // Fetch the specific employee's current details
    fetch(`/ui-api/employees/${id}`)
        .then(response => response.json())
        .then(emp => {
            // Fill the Edit Form
            document.getElementById('editEmpId').value = id;
            document.getElementById('editFname').value = emp.fname;
            document.getElementById('editMinit').value = emp.minit || '';
            document.getElementById('editLname').value = emp.lname;

            // Set Dropdowns and trigger change events to update Job Level validations
            const jobSelect = document.getElementById('editJobSelect');
            jobSelect.value = emp.jobId;
            jobSelect.dispatchEvent(new Event('change'));

            document.getElementById('editJobLvl').value = emp.jobLvl;
            document.getElementById('editPubSelect').value = emp.pubId;

            document.getElementById('editJobLvlError').style.display = 'none';
            document.getElementById('editFormErrorMsg').style.display = 'none';
            document.getElementById('editJobLvl').style.borderColor = '';

            // Open Modal
            document.getElementById('editEmployeeModal').showModal();
        })
        .catch(err => alert("Failed to fetch employee details."));
};

window.viewDetails = function(url) {
    const cleanUrl = url.split('{')[0];
    const id = cleanUrl.split('/').pop();
    window.location.href = `/employee-details?id=${id}`;
};

window.deleteEmployee = function(url) {
    const cleanUrl = url.split('{')[0];
    const id = cleanUrl.split('/').pop();

    if (confirm(`Are you sure you want to delete employee ${id}?`)) {
        fetch(`/ui-api/employees/${id}`, { method: 'DELETE' })
            .then(response => {
                if (response.status === 204 || response.ok) {
                    fetchEmployees(currentUrl);
                } else {
                    alert("Failed to delete. The server rejected the request.");
                }
            }).catch(error => alert("Network error occurred."));
    }
};

// ==========================================
// Core Table & Search Functions
// ==========================================
function handleSearch() {
    clearTimeout(debounceTimer);
    debounceTimer = setTimeout(() => {
        let fname = document.getElementById('searchFname').value.trim();
        let lname = document.getElementById('searchLname').value.trim();
        let jobLvl = document.getElementById('searchJobLvl').value.trim();
        let size = document.getElementById('sizeSelect').value;

        if (fname === "" && lname === "" && jobLvl === "") {
            fetchEmployees(`/ui-api/employees?page=0&size=${size}&sort=fname,asc`);
            return;
        }

        let query = `page=0&size=${size}&sort=fname,asc`;
        if (fname !== "") query += `&fname=${encodeURIComponent(fname)}`;
        if (lname !== "") query += `&lname=${encodeURIComponent(lname)}`;
        if (jobLvl !== "") query += `&jobLvl=${encodeURIComponent(jobLvl)}`;
        fetchEmployees(`/ui-api/employees/search/advanced?${query}`);
    }, 300);
}

function fetchEmployees(url) {
    currentUrl = url;
    fetch(url)
        .then(response => response.json())
        .then(data => {
            let employees = data._embedded ? data._embedded.employees : [];
            renderTable(employees);
            updatePagination(data);
        }).catch(error => console.error("Error fetching data:", error));
}

function renderTable(employees) {
    const tbody = document.getElementById('employeeTableBody');
    tbody.innerHTML = '';
    if (employees.length === 0) {
        tbody.innerHTML = '<tr><td colspan="4">No employees found.</td></tr>';
        return;
    }
    employees.forEach(emp => {
        let middleInitial = (emp.minit && emp.minit.trim() !== '') ? ` ${emp.minit.trim()}.` : '';
        let row = `<tr>
            <td>${emp.fname}${middleInitial}</td>
            <td>${emp.lname}</td>
            <td>${emp.jobLvl}</td>
            <td>
                <button onclick="editEmployee('${emp._links.self.href}')">Edit</button>
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