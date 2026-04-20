// ==================== Star Background ====================
function createStarField() {
    const starField = document.getElementById('starField');
    const starCount = 200;
    
    for (let i = 0; i < starCount; i++) {
        const star = document.createElement('div');
        star.className = 'star';
        
        // Random position
        star.style.left = Math.random() * 100 + '%';
        star.style.top = Math.random() * 100 + '%';
        
        // Random size
        const size = Math.random() * 2 + 1;
        star.style.width = size + 'px';
        star.style.height = size + 'px';
        
        // Random animation
        const duration = Math.random() * 3 + 2;
        star.style.setProperty('--duration', duration + 's');
        star.style.setProperty('--min-opacity', Math.random() * 0.3 + 0.1);
        star.style.setProperty('--max-opacity', Math.random() * 0.5 + 0.5);
        
        starField.appendChild(star);
    }
}

// ==================== Navigation ====================
function initNavigation() {
    const navbar = document.querySelector('.navbar');
    const mobileMenu = document.getElementById('mobileMenu');
    const navLinks = document.querySelector('.nav-links');
    
    // Scroll effect
    window.addEventListener('scroll', () => {
        if (window.scrollY > 50) {
            navbar.classList.add('scrolled');
        } else {
            navbar.classList.remove('scrolled');
        }
    });
    
    // Mobile menu toggle
    if (mobileMenu) {
        mobileMenu.addEventListener('click', () => {
            navLinks.classList.toggle('active');
        });
    }
    
    // Close mobile menu on link click
    document.querySelectorAll('.nav-links a').forEach(link => {
        link.addEventListener('click', () => {
            navLinks.classList.remove('active');
        });
    });
    
    // Smooth scroll for anchor links
    document.querySelectorAll('a[href^="#"]').forEach(anchor => {
        anchor.addEventListener('click', function(e) {
            e.preventDefault();
            const target = document.querySelector(this.getAttribute('href'));
            if (target) {
                target.scrollIntoView({
                    behavior: 'smooth',
                    block: 'start'
                });
            }
        });
    });
}

// ==================== Scroll Animations ====================
function initScrollAnimations() {
    const observerOptions = {
        root: null,
        rootMargin: '0px',
        threshold: 0.1
    };
    
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('visible');
            }
        });
    }, observerOptions);
    
    // Observe all feature cards, enemy cards, etc.
    document.querySelectorAll('.feature-card, .enemy-card, .phase-card, .powerup-card, .control-card, .screenshot-item').forEach(el => {
        el.style.opacity = '0';
        el.style.transform = 'translateY(30px)';
        el.style.transition = 'opacity 0.6s ease, transform 0.6s ease';
        observer.observe(el);
    });
}

// Add visible class styles
const style = document.createElement('style');
style.textContent = `
    .visible {
        opacity: 1 !important;
        transform: translateY(0) !important;
    }
`;
document.head.appendChild(style);

// ==================== Counter Animation ====================
function animateCounters() {
    const counters = document.querySelectorAll('.counter');
    
    const observerOptions = {
        threshold: 0.5
    };
    
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const target = parseInt(entry.target.dataset.target);
                let current = 0;
                const increment = target / 50;
                const timer = setInterval(() => {
                    current += increment;
                    if (current >= target) {
                        entry.target.textContent = target;
                        clearInterval(timer);
                    } else {
                        entry.target.textContent = Math.floor(current);
                    }
                }, 30);
                observer.unobserve(entry.target);
            }
        });
    }, observerOptions);
    
    counters.forEach(counter => {
        observer.observe(counter);
    });
}

// ==================== Screenshot Modal ====================
function initScreenshotModal() {
    const screenshotItems = document.querySelectorAll('.screenshot-item');
    
    screenshotItems.forEach(item => {
        item.addEventListener('click', () => {
            const img = item.querySelector('img');
            const modal = createModal(img.src, img.alt);
            document.body.appendChild(modal);
            setTimeout(() => modal.classList.add('active'), 10);
            
            modal.addEventListener('click', () => {
                modal.classList.remove('active');
                setTimeout(() => modal.remove(), 300);
            });
        });
    });
}

function createModal(src, alt) {
    const modal = document.createElement('div');
    modal.className = 'modal';
    modal.innerHTML = `
        <div class="modal-content">
            <img src="${src}" alt="${alt}">
            <button class="modal-close">&times;</button>
        </div>
    `;
    
    const modalStyle = document.createElement('style');
    modalStyle.textContent = `
        .modal {
            position: fixed;
            top: 0;
            left: 0;
            width: 100%;
            height: 100%;
            background: rgba(0, 0, 0, 0.9);
            display: flex;
            align-items: center;
            justify-content: center;
            z-index: 9999;
            opacity: 0;
            transition: opacity 0.3s ease;
        }
        .modal.active {
            opacity: 1;
        }
        .modal-content {
            position: relative;
            max-width: 90%;
            max-height: 90%;
        }
        .modal-content img {
            max-width: 100%;
            max-height: 90vh;
            border-radius: 12px;
            box-shadow: 0 20px 60px rgba(0, 0, 0, 0.5);
        }
        .modal-close {
            position: absolute;
            top: -40px;
            right: 0;
            background: none;
            border: none;
            color: white;
            font-size: 36px;
            cursor: pointer;
            opacity: 0.7;
            transition: opacity 0.2s;
        }
        .modal-close:hover {
            opacity: 1;
        }
    `;
    modal.appendChild(modalStyle);
    return modal;
}

// ==================== Parallax Effect ====================
function initParallax() {
    window.addEventListener('scroll', () => {
        const scrolled = window.scrollY;
        const heroVisual = document.querySelector('.ship-preview');
        if (heroVisual) {
            heroVisual.style.transform = `translateY(${scrolled * 0.3}px)`;
        }
    });
}

// ==================== Download Counter ====================
function incrementDownloadCount() {
    const countElement = document.getElementById('downloadCount');
    if (countElement) {
        let count = parseInt(localStorage.getItem('downloadCount') || '1247');
        count++;
        localStorage.setItem('downloadCount', count.toString());
        countElement.textContent = count.toLocaleString();
    }
}

// ==================== Initialize ====================
document.addEventListener('DOMContentLoaded', () => {
    createStarField();
    initNavigation();
    initScrollAnimations();
    initScreenshotModal();
    initParallax();
    
    // Display download count
    const countElement = document.getElementById('downloadCount');
    if (countElement) {
        const count = localStorage.getItem('downloadCount') || '1247';
        countElement.textContent = parseInt(count).toLocaleString();
    }
});

// ==================== Easter Egg ====================
let konamiCode = [];
const konamiSequence = ['ArrowUp', 'ArrowUp', 'ArrowDown', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'ArrowLeft', 'ArrowRight', 'b', 'a'];

document.addEventListener('keydown', (e) => {
    konamiCode.push(e.key);
    konamiCode = konamiCode.slice(-10);
    
    if (konamiCode.join(',') === konamiSequence.join(',')) {
        // Easter egg: Play a fun animation
        const ship = document.querySelector('.ship-svg');
        if (ship) {
            ship.style.animation = 'spin 1s ease-in-out';
            setTimeout(() => {
                ship.style.animation = 'float 3s ease-in-out infinite';
            }, 1000);
        }
    }
});

const easterEggStyle = document.createElement('style');
easterEggStyle.textContent = `
    @keyframes spin {
        0% { transform: rotate(0deg); }
        100% { transform: rotate(360deg); }
    }
`;
document.head.appendChild(easterEggStyle);
