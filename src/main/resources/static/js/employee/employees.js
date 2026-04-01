// State variables
let currentUrl = '/ui-api/employees?page=0&size=10&sort=fname,asc';
let prevUrl = null;
let nextUrl = null;
let debounceTimer;
let jobDataMap = {};
let currentDeleteUrl = null;

const empIdRegex = /^([A-Z]{3}[1-9][0-9]{4}[FM]|[A-Z]-[A-Z][1-9][0-9]{4}[FM])$/;

window.toggleModal = function(modalID) {
    const modal = document.getElementById(modalID);
    if (modal) modal.classList.toggle('hidden');
};

document.addEventListener("DOMContentLoaded", () => {
    populateDropdowns();
    fetchEmployees(currentUrl);

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

    document.getElementById('newJobSelect').addEventListener('change', (e) => {
        updateJobLevelHelp(e.target.value, document.getElementById('newJobLvl'), document.getElementById('jobLvlHelp'));
    });

    addForm.addEventListener('submit', (e) => {
        e.preventDefault();

        if (!validateJobLevel(document.getElementById('newJobSelect').value, document.getElementById('newJobLvl'), document.getElementById('jobLvlError'))) return;

        const empIdInput = document.getElementById('newEmpId');
        if (!empIdRegex.test(empIdInput.value.trim())) {
            empIdInput.style.borderColor = '#ef4444';
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

        fetch('/ui-api/employees', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(newEmployee)
        }).then(async response => {
            if (response.ok || response.status === 201) {
                addModal.close();
                fetchEmployees(currentUrl);
            } else {
                const errorText = await response.text();
                if (response.status === 409 || errorText.toLowerCase().includes("already exists") || errorText.toLowerCase().includes("duplicate")) {
                    const empIdError = document.getElementById('empIdError');
                    empIdInput.style.borderColor = '#ef4444';
                    empIdError.innerText = "This Employee ID is already taken in the database.";
                    empIdError.style.display = 'block';
                    document.getElementById('formErrorMsg').style.display = 'none';
                } else {
                    document.getElementById('formErrorMsg').innerHTML = `<i class="fas fa-exclamation-triangle mr-1"></i> <strong>Server Error ${response.status}:</strong> ${errorText || 'The server rejected the data.'}`;
                    document.getElementById('formErrorMsg').style.display = 'block';
                }
            }
        }).catch(error => {
            console.error("Network error:", error);
            document.getElementById('formErrorMsg').innerHTML = `<i class="fas fa-wifi mr-1"></i> Network error occurred. Is the server running?`;
            document.getElementById('formErrorMsg').style.display = 'block';
        });
    });

    document.getElementById('confirmDeleteBtn').addEventListener('click', () => {
        if (currentDeleteUrl) {
            const cleanUrl = currentDeleteUrl.split('{')[0];
            const id = cleanUrl.split('/').pop();

            fetch(`/ui-api/employees/${id}`, { method: 'DELETE' })
                .then(response => {
                    if (response.status === 204 || response.ok) {
                        toggleModal('deleteConfirmModal');
                        fetchEmployees(currentUrl);
                    } else {
                        alert("Failed to delete. The server rejected the request.");
                        toggleModal('deleteConfirmModal');
                    }
                }).catch(error => {
                alert("Network error occurred.");
                toggleModal('deleteConfirmModal');
            });
        }
    });
});

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
            inputElement.style.borderColor = '#ef4444';
            errorElement.innerText = `Must be between ${min} and ${max}.`;
            errorElement.style.display = 'block';
            return false;
        }
    }
    inputElement.style.borderColor = '';
    errorElement.style.display = 'none';
    return true;
}

function populateDropdowns() {
    fetch('/ui-api/employees/reference/jobs')
        .then(res => res.json())
        .then(data => {
            const addJob = document.getElementById('newJobSelect');
            if (data._embedded && data._embedded.jobs) {
                data._embedded.jobs.forEach(job => {
                    const jobId = job._links.self.href.split('/').pop();
                    addJob.add(new Option(job.jobDesc, jobId));
                    jobDataMap[jobId] = { minLvl: job.minLvl, maxLvl: job.maxLvl };
                });
            }
        });

    fetch('/ui-api/employees/reference/publishers')
        .then(res => res.json())
        .then(data => {
            const addPub = document.getElementById('newPubSelect');
            if (data._embedded && data._embedded.publishers) {
                data._embedded.publishers.forEach(pub => {
                    const pubId = pub._links.self.href.split('/').pop();
                    addPub.add(new Option(pub.pubName, pubId));
                });
            }
        });
}

window.viewDetails = function(url) {
    const cleanUrl = url.split('{')[0];
    const id = cleanUrl.split('/').pop();
    window.location.href = `/employee-details?id=${id}`;
};

// 🔧 FIX: Now specifically routes through the proxy controller to parse the date properly!
window.prepareDelete = function(url, empName, event) {
    event.stopPropagation();
    currentDeleteUrl = url;

    const cleanUrl = url.split('{')[0];
    const empId = cleanUrl.split('/').pop();

    document.getElementById('deleteEmpNameModal').innerText = empName;
    document.getElementById('deleteEmpIdModal').innerText = empId;
    document.getElementById('deleteEmpDateModal').innerHTML = '<i class="fas fa-spinner fa-spin text-slate-300"></i>';
    toggleModal('deleteConfirmModal');

    // Routing through /ui-api/ ensures we get the nicely formatted JSON
    fetch(`/ui-api/employees/${empId}`)
        .then(res => res.json())
        .then(emp => {
            if (emp.hireDate) {
                const dateObj = new Date(emp.hireDate);
                if (!isNaN(dateObj)) {
                    document.getElementById('deleteEmpDateModal').innerText = dateObj.toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' });
                } else {
                    document.getElementById('deleteEmpDateModal').innerText = 'Invalid Date format';
                }
            } else {
                document.getElementById('deleteEmpDateModal').innerText = 'Date Unknown';
            }
        })
        .catch(() => {
            document.getElementById('deleteEmpDateModal').innerText = 'Error fetching date';
        });
};

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
        tbody.innerHTML = `<tr><td colspan="4" class="p-12 text-center text-slate-500"><i class="fas fa-users-slash text-4xl text-slate-300 mb-3 block"></i>No employees found.</td></tr>`;
        return;
    }
    employees.forEach(emp => {
        let middleInitial = (emp.minit && emp.minit.trim() !== '') ? ` ${emp.minit.trim()}.` : '';
        let fullName = `${emp.fname}${middleInitial} ${emp.lname}`;

        // 🎨 UI FIX: Restored the red background to match the dustbin design exactly
        let row = `<tr title="Click to view employee profile" class="hover:bg-slate-50 transition-all duration-200 group cursor-pointer" onclick="viewDetails('${emp._links.self.href}')">
            <td class="p-4 font-bold text-slate-800 group-hover:text-indigo-600 transition relative">
                ${emp.fname}${middleInitial}
                <i class="fas fa-chevron-right absolute right-4 top-1/2 -translate-y-1/2 opacity-0 group-hover:opacity-100 group-hover:translate-x-1 transition-all text-indigo-400 text-xs"></i>
            </td>
            <td class="p-4 text-sm font-medium text-slate-700">${emp.lname}</td>
            <td class="p-4 text-center text-sm font-medium text-slate-700">
                <span class="bg-indigo-50 text-indigo-700 border border-indigo-200 px-2.5 py-1 rounded-md font-mono text-xs">${emp.jobLvl}</span>
            </td>
            <td class="p-4 text-center" onclick="event.stopPropagation()">
                <button title="Delete Employee" onclick="prepareDelete('${emp._links.self.href}', '${fullName.replace(/'/g, "\\'")}', event)" class="bg-red-50 text-red-600 hover:bg-red-100 font-bold py-1.5 px-3 rounded-lg text-sm transition shadow-sm inline-flex items-center justify-center">
                    <i class="fas fa-trash-alt"></i>
                </button>
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

    pageInfo.innerHTML = `Showing page <strong class="text-slate-900">${data.page.number + 1}</strong> of <strong class="text-slate-900">${data.page.totalPages}</strong> (Total: ${data.page.totalElements})`;
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