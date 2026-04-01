let jobDataMap = {};
const params = new URLSearchParams(window.location.search);
const empId = params.get('id');

window.toggleModal = function(modalID) {
    const modal = document.getElementById(modalID);
    if (modal) modal.classList.toggle('hidden');
};

document.addEventListener("DOMContentLoaded", () => {
    if (!empId) {
        document.getElementById('detailsContainer').innerHTML = "<p class='text-red-500 font-bold'><i class='fas fa-exclamation-circle'></i> No Employee ID provided.</p>";
        return;
    }

    populateDropdowns();
    loadEmployeeDetails();

    const editModal = document.getElementById('editEmployeeModal');
    const editForm = document.getElementById('editEmployeeForm');

    document.getElementById('closeEditModalBtn').addEventListener('click', () => {
        editModal.close();
    });

    document.getElementById('editJobSelect').addEventListener('change', (e) => {
        updateJobLevelHelp(e.target.value, document.getElementById('editJobLvl'), document.getElementById('editJobLvlHelp'));
    });

    editForm.addEventListener('submit', (e) => {
        e.preventDefault();

        if (!validateJobLevel(document.getElementById('editJobSelect').value, document.getElementById('editJobLvl'), document.getElementById('editJobLvlError'))) return;

        const updates = {
            fname: document.getElementById('editFname').value.trim(),
            minit: document.getElementById('editMinit').value.trim() || null,
            lname: document.getElementById('editLname').value.trim(),
            jobId: parseInt(document.getElementById('editJobSelect').value, 10),
            jobLvl: parseInt(document.getElementById('editJobLvl').value, 10),
            pubId: document.getElementById('editPubSelect').value
        };

        fetch(`/ui-api/employees/${empId}`, {
            method: 'PATCH',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updates)
        }).then(async response => {
            if (response.ok || response.status === 200) {
                editModal.close();
                loadEmployeeDetails();
            } else {
                const errorText = await response.text();
                document.getElementById('editFormErrorMsg').innerHTML = `<i class="fas fa-exclamation-triangle"></i> <strong>Server Error ${response.status}:</strong> ${errorText}`;
                document.getElementById('editFormErrorMsg').style.display = 'block';
            }
        }).catch(error => {
            console.error("Network error:", error);
            document.getElementById('editFormErrorMsg').innerHTML = `<i class="fas fa-wifi"></i> Network error occurred.`;
            document.getElementById('editFormErrorMsg').style.display = 'block';
        });
    });

    document.getElementById('confirmDeleteBtn').addEventListener('click', () => {
        fetch(`/ui-api/employees/${empId}`, { method: 'DELETE' })
            .then(response => {
                if (response.status === 204 || response.ok) {
                    window.location.href = '/employees';
                } else {
                    alert("Failed to delete. The server rejected the request.");
                    toggleModal('deleteConfirmModal');
                }
            }).catch(error => {
            alert("Network error occurred.");
            toggleModal('deleteConfirmModal');
        });
    });
});

function loadEmployeeDetails() {
    const container = document.getElementById('detailsContainer');
    fetch(`/ui-api/employees/${empId}`)
        .then(async response => {
            if (response.status === 404) {
                throw new Error(`Employee ${empId} was not found. They may have been deleted.`);
            }
            if (!response.ok) {
                throw new Error(`Server returned error: ${response.status}`);
            }
            return response.json();
        })
        .then(emp => {
            const middleInitial = (emp.minit && emp.minit.trim() !== '') ? ` ${emp.minit.trim()}.` : '';
            const fullName = `${emp.fname}${middleInitial} ${emp.lname}`;
            const jobDesc = emp.job ? emp.job.jobDesc : 'Data Missing';
            const pubName = emp.publisher ? emp.publisher.pubName : 'Data Missing';

            // Format date for display and the modal
            const formattedHireDate = emp.hireDate ? new Date(emp.hireDate).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' }) : 'Unknown Date';

            container.innerHTML = `
                <div class="grid grid-cols-1 lg:grid-cols-3 gap-8">
                    <div class="lg:col-span-1 space-y-6">
                        <div class="bg-white p-8 rounded-3xl border border-slate-100 shadow-sm text-center relative overflow-hidden">
                            <div class="absolute top-0 left-0 right-0 h-24 hero-gradient"></div>
                            
                            <div class="relative w-24 h-24 mx-auto mt-8 mb-4 bg-white rounded-full p-1 shadow-md">
                                <div class="w-full h-full bg-slate-100 rounded-full flex items-center justify-center text-3xl text-indigo-400">
                                    <i class="fas fa-user-tie"></i>
                                </div>
                            </div>

                            <h2 class="text-2xl font-extrabold text-slate-900 line-clamp-2">${fullName}</h2>
                            <p class="text-sm font-mono text-slate-500 mt-2">${empId}</p>

                            <ul class="text-left space-y-4 text-sm text-slate-600 border-t border-slate-100 pt-6 mt-6">
                                <li class="flex items-start">
                                    <i class="fas fa-calendar-alt text-indigo-400 w-6 mt-1"></i> 
                                    <div>
                                        <span class="block text-xs font-bold text-slate-400 uppercase tracking-wider">Hire Date</span>
                                        <span class="block font-medium mt-1 text-slate-800">${formattedHireDate}</span>
                                    </div>
                                </li>
                            </ul>

                            <div class="grid grid-cols-2 gap-3 mt-8 pt-6 border-t border-slate-100">
                                <button id="openEditBtn" class="bg-slate-100 text-slate-700 hover:bg-slate-200 py-2.5 rounded-xl font-bold transition shadow-sm w-full">
                                    <i class="fas fa-edit mr-1"></i> Edit
                                </button>
                                <button id="openDeleteBtn" class="bg-red-50 text-red-600 hover:bg-red-100 py-2.5 rounded-xl font-bold transition shadow-sm w-full">
                                    <i class="fas fa-trash-alt mr-1"></i> Delete
                                </button>
                            </div>
                        </div>
                    </div>

                    <div class="lg:col-span-2 space-y-6">
                        <div class="bg-white rounded-3xl border border-slate-100 shadow-sm overflow-hidden">
                            <div class="p-6 border-b border-slate-100 bg-slate-50/50 flex items-center">
                                <h3 class="text-lg font-bold text-slate-900"><i class="fas fa-briefcase text-indigo-500 mr-2"></i> Professional Details</h3>
                            </div>
                            <div class="p-6">
                                <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                                    <div class="flex items-center p-4 bg-slate-50 rounded-xl border border-slate-100">
                                        <div class="w-10 h-10 rounded-full bg-indigo-100 text-indigo-500 flex items-center justify-center flex-shrink-0"><i class="fas fa-star"></i></div>
                                        <div class="ml-4">
                                            <p class="text-xs text-slate-500 font-bold uppercase tracking-wider">Job Role</p>
                                            <p class="font-bold text-slate-900 mt-1">${jobDesc}</p>
                                        </div>
                                    </div>
                                    <div class="flex items-center p-4 bg-slate-50 rounded-xl border border-slate-100">
                                        <div class="w-10 h-10 rounded-full bg-emerald-100 text-emerald-500 flex items-center justify-center flex-shrink-0"><i class="fas fa-layer-group"></i></div>
                                        <div class="ml-4">
                                            <p class="text-xs text-slate-500 font-bold uppercase tracking-wider">Job Level</p>
                                            <p class="font-bold text-slate-900 mt-1 font-mono">${emp.jobLvl}</p>
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </div>

                        <div class="bg-white rounded-3xl border border-slate-100 shadow-sm overflow-hidden">
                            <div class="p-6 border-b border-slate-100 bg-slate-50/50 flex items-center">
                                <h3 class="text-lg font-bold text-slate-900"><i class="fas fa-building text-indigo-500 mr-2"></i> Assigned Publisher</h3>
                            </div>
                            <div class="p-6">
                                <div class="flex items-center p-4 bg-slate-50 rounded-xl border border-slate-100">
                                    <div class="w-10 h-10 rounded-full bg-amber-100 text-amber-500 flex items-center justify-center flex-shrink-0"><i class="fas fa-city"></i></div>
                                    <div class="ml-4">
                                        <p class="text-xs text-slate-500 font-bold uppercase tracking-wider">Publisher Name</p>
                                        <p class="font-bold text-slate-900 mt-1">${pubName}</p>
                                    </div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            `;

            document.getElementById('openEditBtn').addEventListener('click', () => {
                openEditModal(empId);
            });

            // Populate the detailed modal card
            document.getElementById('openDeleteBtn').addEventListener('click', () => {
                document.getElementById('deleteEmpNameModal').innerText = fullName;
                document.getElementById('deleteEmpIdModal').innerText = empId;

                const shortDate = emp.hireDate ? new Date(emp.hireDate).toLocaleDateString('en-US', { year: 'numeric', month: 'short', day: 'numeric' }) : 'Unknown';
                document.getElementById('deleteEmpDateModal').innerText = shortDate;

                toggleModal('deleteConfirmModal');
            });
        })
        .catch(err => {
            console.error(err);
            container.innerHTML = `
                <div class="bg-red-50 border border-red-200 text-red-600 p-6 rounded-2xl text-center">
                    <i class="fas fa-exclamation-triangle text-3xl mb-3"></i>
                    <p class="font-bold">${err.message}</p>
                    <button onclick="window.history.back()" class="mt-4 bg-white border border-red-200 text-red-600 px-4 py-2 rounded-lg text-sm font-bold hover:bg-red-50 transition">Go Back</button>
                </div>`;
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