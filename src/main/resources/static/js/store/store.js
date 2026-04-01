$(document).ready(function() {
        $('#storeTable').DataTable({
            "pageLength": 10,  // Defaults to showing 10 rows
            "lengthMenu": [ [3, 5, 10, 25, 50], [3, 5, 10, 25, 50] ],
            "ordering": true,
            "info": true,
            "lengthChange": true,
            "searching": true,
            "columnDefs": [
                { "width": "45%", "targets": 0 },
                { "width": "25%", "targets": 1 },
                { "width": "15%", "targets": 2 },
                { "orderable": false, "targets": 3, "width": "15%" }
            ],
            "language": {
                "search": "Search Anything:",
                "paginate": {
                    "previous": "<i class='fas fa-chevron-left me-1'></i> Prev",
                    "next": "Next <i class='fas fa-chevron-right ms-1'></i>"
                }
            }
        });

        // Modal handlers
        if ($('#addStoreModal .is-invalid').length > 0) {
            var addModal = new bootstrap.Modal(document.getElementById('addStoreModal'));
            addModal.show();
        }

        $('#addStoreModal .is-invalid').on('input', function() {
            $(this).removeClass('is-invalid');
        });

        $('#addStoreModal').on('hidden.bs.modal', function () {
            $(this).find('form')[0].reset();
            $(this).find('.is-invalid').removeClass('is-invalid');
        });
    });

    function populateEditModal(button) {
        document.getElementById('editStorId').value = button.getAttribute('data-id');
        document.getElementById('editStorName').value = button.getAttribute('data-name');
        document.getElementById('editStorAddress').value = button.getAttribute('data-address');
        document.getElementById('editCity').value = button.getAttribute('data-city');
        document.getElementById('editState').value = button.getAttribute('data-state');
        document.getElementById('editZip').value = button.getAttribute('data-zip');
    }

    function prepareDeleteModal(button) {
        const storeId = button.getAttribute('data-id');
        const storeName = button.getAttribute('data-name');
        document.getElementById('deleteStoreName').innerText = storeName;
        document.getElementById('confirmDeleteBtn').href = '/stores/delete/' + storeId;
        var deleteModal = new bootstrap.Modal(document.getElementById('deleteConfirmModal'));
        deleteModal.show();
    }