const header = document.querySelector("[data-header]");
const progressBar = document.querySelector(".progress-bar");
const menuButton = document.querySelector(".menu-button");
const mobileNav = document.querySelector("#mobile-nav");
const revealItems = document.querySelectorAll(".reveal");
const countItems = document.querySelectorAll("[data-count]");
const magneticItems = document.querySelectorAll(".magnetic");
const spotlightCards = document.querySelectorAll(".spotlight");
const roleTabButtons = document.querySelectorAll("[data-role-tab]");
const rolePanels = document.querySelectorAll("[data-role-panel]");
const bookingDialog = document.querySelector("#booking-dialog");
const bookingOpenButtons = document.querySelectorAll("[data-open-booking]");
const bookingForm = bookingDialog?.querySelector("form");
const demoToast = document.querySelector(".demo-toast");
const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;

function updateScrollState() {
  const scrollTop = window.scrollY || document.documentElement.scrollTop;
  const maxScroll = document.documentElement.scrollHeight - window.innerHeight;
  const progress = maxScroll > 0 ? (scrollTop / maxScroll) * 100 : 0;

  progressBar.style.width = `${Math.min(100, Math.max(0, progress))}%`;
  header.classList.toggle("is-scrolled", scrollTop > 18);
}

function formatCount(value) {
  if (value >= 1000) return value.toLocaleString("zh-CN");
  return String(value);
}

function animateCount(node) {
  if (node.dataset.done) return;
  node.dataset.done = "true";

  const target = Number(node.dataset.count || 0);
  const suffix = target === 98 ? "%" : "";

  if (reduceMotion) {
    node.textContent = `${formatCount(target)}${suffix}`;
    return;
  }

  const start = performance.now();
  const duration = 1150;

  function tick(now) {
    const rawProgress = Math.min((now - start) / duration, 1);
    const eased = 1 - Math.pow(1 - rawProgress, 4);
    const current = Math.round(target * eased);
    node.textContent = `${formatCount(current)}${suffix}`;

    if (rawProgress < 1) requestAnimationFrame(tick);
  }

  requestAnimationFrame(tick);
}

const observer = new IntersectionObserver(
  (entries) => {
    entries.forEach((entry) => {
      if (!entry.isIntersecting) return;

      entry.target.classList.add("is-visible");
      entry.target.querySelectorAll("[data-count]").forEach(animateCount);
      if (entry.target.matches("[data-count]")) animateCount(entry.target);
      observer.unobserve(entry.target);
    });
  },
  { threshold: 0.18, rootMargin: "0px 0px -8% 0px" },
);

revealItems.forEach((item) => observer.observe(item));
countItems.forEach((item) => observer.observe(item));

menuButton.addEventListener("click", () => {
  const expanded = menuButton.getAttribute("aria-expanded") === "true";
  menuButton.setAttribute("aria-expanded", String(!expanded));
  mobileNav.hidden = expanded;
});

mobileNav.querySelectorAll("a").forEach((link) => {
  link.addEventListener("click", () => {
    menuButton.setAttribute("aria-expanded", "false");
    mobileNav.hidden = true;
  });
});

function activateRole(role) {
  roleTabButtons.forEach((button) => {
    const selected = button.dataset.roleTab === role;
    button.setAttribute("aria-selected", String(selected));
    button.tabIndex = selected ? 0 : -1;
  });

  rolePanels.forEach((panel) => {
    const selected = panel.dataset.rolePanel === role;
    panel.hidden = !selected;
    panel.classList.toggle("is-active", selected);
  });
}

roleTabButtons.forEach((button, index) => {
  button.addEventListener("click", () => activateRole(button.dataset.roleTab));
  button.addEventListener("keydown", (event) => {
    if (!["ArrowRight", "ArrowLeft", "Home", "End"].includes(event.key)) return;
    event.preventDefault();

    let nextIndex = index;
    if (event.key === "ArrowRight") nextIndex = (index + 1) % roleTabButtons.length;
    if (event.key === "ArrowLeft") nextIndex = (index - 1 + roleTabButtons.length) % roleTabButtons.length;
    if (event.key === "Home") nextIndex = 0;
    if (event.key === "End") nextIndex = roleTabButtons.length - 1;

    roleTabButtons[nextIndex].focus();
    activateRole(roleTabButtons[nextIndex].dataset.roleTab);
  });
});

bookingOpenButtons.forEach((button) => {
  button.addEventListener("click", () => {
    if (!bookingDialog) return;
    if (typeof bookingDialog.showModal === "function") {
      bookingDialog.showModal();
      return;
    }
    bookingDialog.setAttribute("open", "");
  });
});

bookingForm?.addEventListener("submit", (event) => {
  if (event.submitter?.dataset.bookingSubmit === undefined) return;
  demoToast.hidden = false;
  window.clearTimeout(demoToast.dataset.timer);
  demoToast.dataset.timer = window.setTimeout(() => {
    demoToast.hidden = true;
  }, 4200);
});

spotlightCards.forEach((card) => {
  card.addEventListener("pointermove", (event) => {
    const rect = card.getBoundingClientRect();
    card.style.setProperty("--mouse-x", `${event.clientX - rect.left}px`);
    card.style.setProperty("--mouse-y", `${event.clientY - rect.top}px`);
  });
});

if (!reduceMotion) {
  magneticItems.forEach((item) => {
    item.addEventListener("pointermove", (event) => {
      const rect = item.getBoundingClientRect();
      const x = event.clientX - rect.left - rect.width / 2;
      const y = event.clientY - rect.top - rect.height / 2;
      item.style.transform = `translate(${x * 0.08}px, ${y * 0.12}px)`;
    });

    item.addEventListener("pointerleave", () => {
      item.style.transform = "";
    });
  });
}

window.addEventListener("scroll", updateScrollState, { passive: true });
window.addEventListener("resize", updateScrollState);
updateScrollState();

document.documentElement.classList.add("js-ready");
