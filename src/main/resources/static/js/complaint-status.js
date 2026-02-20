        document.querySelectorAll('.star-rating').forEach((ratingContainer) => {
            const complaintId = ratingContainer.getAttribute('data-complaint-id');
            const stars = ratingContainer.querySelectorAll('.star');
            const ratingValue = document.getElementById('ratingValue-' + complaintId);

            if (!ratingValue) {
                return;
            }

            function paintStars(value) {
                const numericValue = Number(value) || 0;
                stars.forEach((star) => {
                    const starValue = Number(star.getAttribute('data-value'));
                    star.classList.toggle('active', starValue <= numericValue);
                });
            }

            stars.forEach((star) => {
                star.addEventListener('click', function () {
                    const value = Number(this.getAttribute('data-value'));
                    ratingValue.value = String(value);
                    paintStars(value);
                });

                star.addEventListener('mouseenter', function () {
                    const value = Number(this.getAttribute('data-value'));
                    paintStars(value);
                });
            });

            ratingContainer.addEventListener('mouseleave', function () {
                paintStars(ratingValue.value);
            });

            paintStars(ratingValue.value);
        });

        document.querySelectorAll('.feedback-form').forEach((form) => {
            form.addEventListener('submit', function (event) {
                const ratingInput = form.querySelector('input[name="rating"]');
                if (!ratingInput || Number(ratingInput.value) < 1) {
                    event.preventDefault();
                    alert('Please select a star rating before submitting feedback.');
                }
            });
        });
    
