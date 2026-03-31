document.addEventListener("DOMContentLoaded", () => {
    // 1. Grab the Employee ID from the browser's URL bar (e.g., ?id=PTC11962M)
    const params = new URLSearchParams(window.location.search);
    const id = params.get('id');

    const container = document.getElementById('detailsContainer');

    if (!id) {
        container.innerHTML = "<p style='color: red;'>No Employee ID provided.</p>";
        return;
    }

    // 2. Fetch the detailed data using API 8 from your Proxy Controller
    fetch(`/ui-api/employees/${id}`)
        .then(async response => {
            if (response.status === 404) {
                throw new Error(`Employee ${id} was not found. They may have been deleted or deactivated.`);
            }
            if (!response.ok) {
                throw new Error(`Server returned error: ${response.status}`);
            }
            return response.json();
        })
        .then(emp => {
            // Format the middle initial safely
            const middleInitial = (emp.minit && emp.minit.trim() !== '') ? ` ${emp.minit.trim()}.` : '';

            // 3. Inject the joined data directly into the HTML
            container.innerHTML = `
                <table border="1" cellpadding="8" style="border-collapse: collapse; margin-top: 15px;">
                    <tr>
                        <th style="background-color: #f4f4f4; text-align: left;">Employee ID</th>
                        <td>${id}</td>
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
        })
        .catch(err => {
            console.error(err);
            container.innerHTML = `<p style='color: red; font-weight: bold;'>${err.message}</p>`;
        });
});