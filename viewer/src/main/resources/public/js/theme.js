(function () {
    var saved = localStorage.getItem('theme');
    var prefersDark = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
    var isDark = saved === 'dark' || (!saved && prefersDark);

    if (isDark) {
        document.documentElement.classList.add('dark');
    }

    // Keep flame.html's separate key in sync so it picks up the app-wide preference
    if (saved) {
        localStorage.setItem('flame-theme', saved);
    }

    var SUN_SVG = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>';
    var MOON_SVG = '<svg xmlns="http://www.w3.org/2000/svg" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/></svg>';

    function applyTheme(dark) {
        if (dark) {
            document.documentElement.classList.add('dark');
        } else {
            document.documentElement.classList.remove('dark');
        }
        updateButtons(dark);
    }

    function updateButtons(dark) {
        document.querySelectorAll('[data-theme-toggle]').forEach(function (btn) {
            var iconEl = btn.querySelector('[data-theme-toggle-icon]');
            if (iconEl) {
                iconEl.innerHTML = dark ? SUN_SVG : MOON_SVG;
            } else {
                btn.innerHTML = dark ? SUN_SVG : MOON_SVG;
            }
            btn.setAttribute('aria-label', dark ? 'Switch to light mode' : 'Switch to dark mode');
        });
    }

    function propagateToIframes(dark) {
        document.querySelectorAll('iframe').forEach(function (frame) {
            try {
                var doc = frame.contentDocument;
                if (doc && doc.documentElement) {
                    if (dark) {
                        doc.documentElement.classList.add('dark');
                    } else {
                        doc.documentElement.classList.remove('dark');
                    }
                }
            } catch (e) {}
        });
    }

    function toggle() {
        var dark = document.documentElement.classList.toggle('dark');
        var value = dark ? 'dark' : 'light';
        localStorage.setItem('theme', value);
        localStorage.setItem('flame-theme', value);
        updateButtons(dark);
        propagateToIframes(dark);
    }

    // React to theme changes made by the parent page (storage event fires in other same-origin windows)
    window.addEventListener('storage', function (e) {
        if (e.key === 'theme') {
            applyTheme(e.newValue === 'dark');
        }
    });

    document.addEventListener('DOMContentLoaded', function () {
        updateButtons(document.documentElement.classList.contains('dark'));
        document.querySelectorAll('[data-theme-toggle]').forEach(function (btn) {
            btn.addEventListener('click', toggle);
        });
    });
}());
