/**
 * Executes upon document readiness to initialize UI components and handle
 * validation state restorations.
 */
$(document).ready(function() {

    // Initialize DataTables for the stores directory with customized layout
    $('#storeTable').DataTable({
        "pageLength": 10,
        "ordering": true,
        "info": true,
        "lengthChange": true,
        "searching": true,
        "columnDefs": [
            { "orderable": false, "targets": 5 } // Disable sorting on Actions column
        ],
        "language": {
            "search": "Search Anything:"
        }
    });

    // Automatically reopen the Add Store modal if server-side validation fails
    if ($('#addStoreModal .is-invalid').length > 0) {
        var addModal = new bootstrap.Modal(document.getElementById('addStoreModal'));
        addModal.show();
    }

    // Remove validation error styling dynamically as the user corrects input
    $('#addStoreModal .is-invalid').on('input', function() {
        $(this).removeClass('is-invalid');
    });

    // Reset the Add Store form state entirely upon modal closure
    $('#addStoreModal').on('hidden.bs.modal', function () {
        $(this).find('form')[0].reset();
        $(this).find('.is-invalid').removeClass('is-invalid');
    });
});

/**
 * Populates the Edit Store modal with data extracted from the selected table row.
 * * @param {HTMLElement} button - The button element triggering the modal, containing data attributes.
 */
function populateEditModal(button) {
    document.getElementById('editStorId').value = button.getAttribute('data-id');
    document.getElementById('editStorName').value = button.getAttribute('data-name');
    document.getElementById('editStorAddress').value = button.getAttribute('data-address');
    document.getElementById('editCity').value = button.getAttribute('data-city');
    document.getElementById('editState').value = button.getAttribute('data-state');
    document.getElementById('editZip').value = button.getAttribute('data-zip');
}

/**
 * Prepares and launches the custom Delete Confirmation modal.
 * Injects the specific store context into the modal UI before displaying.
 * * @param {HTMLElement} button - The delete button element containing data attributes.
 */
function prepareDeleteModal(button) {
    const storeId = button.getAttribute('data-id');
    const storeName = button.getAttribute('data-name');

    // Inject store name into the confirmation text
    document.getElementById('deleteStoreName').innerText = storeName;

    // Attach the correct API endpoint to the confirmation button
    document.getElementById('confirmDeleteBtn').href = '/stores/delete/' + storeId;

    // Trigger the Bootstrap modal
    var deleteModal = new bootstrap.Modal(document.getElementById('deleteConfirmModal'));
    deleteModal.show();
}