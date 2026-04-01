let jobDataMap = {};
const params = new URLSearchParams(window.location.search);
const empId = params.get('id');

document.addEventListener("DOMContentLoaded", () => {
    if (!empId) {
        document.getElementById('detailsContainer').innerHTML = "<p style='color: var(--danger-color); font-weight: bold;'>No Employee ID provided.</p>";
        return;
    }

    // 1. Fetch dropdowns so the Edit Modal is ready behind the scenes
    populateDropdowns();

    // 2. Fetch and display the employee's details
    loadEmployeeDetails();

    // 3. Setup Edit Modal Listeners
    const editModal = document.getElementById('editEmployeeModal');
    const editForm = document.getElementById('editEmployeeForm');

    document.getElementById('openEditBtn').addEventListener('click', () => {
        openEditModal(empId);
    });

    document.getElementById('closeEditModalBtn').addEventListener('click', () => {
        editModal.close();
    });

    document.getElementById('editJobSelect').addEventListener('change', (e) => {
        updateJobLevelHelp(e.target.value, document.getElementById('editJobLvl'), document.getElementById('editJobLvlHelp'));
    });

    editForm.addEventListener('submit', (e) => {
        e.preventDefault();

        // Run validation
        if (!validateJobLevel(document.getElementById('editJobSelect').value, document.getElementById('editJobLvl'), document.getElementById('editJobLvlError'))) return;

        // Build Payload
        const updates = {
            fname: document.getElementById('editFname').value.trim(),
            minit: document.getElementById('editMinit').value.trim() || null,
            lname: document.getElementById('editLname').value.trim(),
            jobId: parseInt(document.getElementById('editJobSelect').value, 10),
            jobLvl: parseInt(document.getElementById('editJobLvl').value, 10),
            pubId: document.getElementById('editPubSelect').value
        };

        // Send PATCH request
        fetch(`/ui-api/employees/${empId}`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updates)
        }).then(async response => {
            if (response.ok || response.status === 200) {
                editModal.close();
                // Magic: Refresh the profile page data instantly without a hard reload!
                loadEmployeeDetails();
            } else {
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
// DATA LOADING
// ==========================================
function loadEmployeeDetails() {
    const container = document.getElementById('detailsContainer');
    fetch(`/ui-api/employees/${empId}`)
        .then(async response => {
            if (response.status === 404) {
                throw new Error(`Employee ${empId} was not found. They may have been deleted or deactivated.`);
            }
            if (!response.ok) {
                throw new Error(`Server returned error: ${response.status}`);
            }
            return response.json();
        })
        .then(emp => {
            const middleInitial = (emp.minit && emp.minit.trim() !== '') ? ` ${emp.minit.trim()}.` : '';
            container.innerHTML = `
                <table border="1" cellpadding="8" style="border-collapse: collapse; margin-top: 15px; width: 100%;">
                    <tr>
                        <th style="background-color: #f4f4f4; text-align: left; width: 30%;">Employee ID</th>
                        <td>${empId}</td>
                    </tr>
                    <tr>
                        <th style="background-color: #f4f4f4; text-align: left;">Full Name</th>
                        <td>${emp.fname}${middleInitial} ${emp.lname}</td>
                    </tr>
                    <tr>
                        <th style="background-color: #f4f4f4; text-align: left;">Hire Date</th>
                        <td>${emp.hireDate}</td>
                    </tr>
                    <tr>
                        <th style="background-color: #f4f4f4; text-align: left;">Job Role</th>
                        <td>${emp.job ? emp.job.jobDesc : 'Data Missing'}</td>
                    </tr>
                    <tr>
                        <th style="background-color: #f4f4f4; text-align: left;">Job Level</th>
                        <td>${emp.jobLvl}</td>
                    </tr>
                    <tr>
                        <th style="background-color: #f4f4f4; text-align: left;">Publisher</th>
                        <td>${emp.publisher ? emp.publisher.pubName : 'Data Missing'}</td>
                    </tr>
                </table>
            `;
            // Only show the edit button if the data loaded successfully
            document.getElementById('openEditBtn').style.display = 'inline-block';
        })
        .catch(err => {
            console.error(err);
            container.innerHTML = `<p style='color: var(--danger-color); font-weight: bold;'>${err.message}</p>`;
            document.getElementById('openEditBtn').style.display = 'none';
        });
}

function openEditModal(id) {
    fetch(`/ui-api/employees/${id}`)
        .then(response => response.json())
        .then(emp => {
            document.getElementById('editEmpId').value = id;
            document.getElementById('editFname').value = emp.fname;
            document.getElementById('editMinit').value = emp.minit || '';
            document.getElementById('editLname').value = emp.lname;

            // HATEOAS Extraction: Since the projection hides raw IDs, we extract them from the hypermedia links!
            let actualJobId = "";
            if (emp._links && emp._links.job && emp._links.job.href) {
                actualJobId = emp._links.job.href.split('{')[0].split('/').pop();
            }

            let actualPubId = "";
            if (emp._links && emp._links.publisher && emp._links.publisher.href) {
                actualPubId = emp._links.publisher.href.split('{')[0].split('/').pop();
            }

            const jobSelect = document.getElementById('editJobSelect');
            jobSelect.value = actualJobId;
            jobSelect.dispatchEvent(new Event('change'));

            document.getElementById('editJobLvl').value = emp.jobLvl;
            document.getElementById('editPubSelect').value = actualPubId;

            document.getElementById('editJobLvlError').style.display = 'none';
            document.getElementById('editFormErrorMsg').style.display = 'none';
            document.getElementById('editJobLvl').style.borderColor = '';

            document.getElementById('editEmployeeModal').showModal();
        })
        .catch(err => alert("Failed to fetch employee details."));
}

// ==========================================
// HELPERS
// ==========================================
function populateDropdowns() {
    fetch('/ui-api/employees/reference/jobs')
        .then(res => res.json())
        .then(data => {
            const editJob = document.getElementById('editJobSelect');
            if (data._embedded && data._embedded.jobs) {
                data._embedded.jobs.forEach(job => {
                    const jobId = job._links.self.href.split('/').pop();
                    editJob.add(new Option(job.jobDesc, jobId));
                    jobDataMap[jobId] = { minLvl: job.minLvl, maxLvl: job.maxLvl };
                });
            }
        });

    fetch('/ui-api/employees/reference/publishers')
        .then(res => res.json())
        .then(data => {
            const editPub = document.getElementById('editPubSelect');
            if (data._embedded && data._embedded.publishers) {
                data._embedded.publishers.forEach(pub => {
                    const pubId = pub._links.self.href.split('/').pop();
                    editPub.add(new Option(pub.pubName, pubId));
                });
            }
        });
}

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