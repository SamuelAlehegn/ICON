import { Directive, ElementRef, Input, OnDestroy, OnInit, Renderer2, inject } from '@angular/core';

@Directive({
  selector: 'img[appLazyImage]',
  standalone: true
})
export class LazyImageDirective implements OnInit, OnDestroy {
  private readonly element = inject<ElementRef<HTMLImageElement>>(ElementRef);
  private readonly renderer = inject(Renderer2);

  private src: string | null = null;
  private initialised = false;
  private requested = false;
  private observer?: IntersectionObserver;

  @Input('appLazyImage')
  set appLazyImage(value: string | null | undefined) {
    this.src = value || null;
    if (this.initialised) {
      this.observeOrLoad();
    }
  }

  ngOnInit(): void {
    this.initialised = true;
    this.renderer.addClass(this.element.nativeElement, 'lazy-loading');
    this.observeOrLoad();
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }

  private observeOrLoad(): void {
    if (!this.src || this.requested) {
      return;
    }

    if (typeof IntersectionObserver === 'undefined') {
      this.loadImage();
      return;
    }

    this.observer?.disconnect();
    this.observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((entry) => entry.isIntersecting)) {
          this.loadImage();
          this.observer?.disconnect();
        }
      },
      { rootMargin: '200px 0px' }
    );
    this.observer.observe(this.element.nativeElement);
  }

  private loadImage(): void {
    const src = this.src;
    if (!src || this.requested) {
      return;
    }

    this.requested = true;
    const image = new Image();
    image.onload = () => {
      this.renderer.setAttribute(this.element.nativeElement, 'src', src);
      this.renderer.removeClass(this.element.nativeElement, 'lazy-loading');
      this.renderer.addClass(this.element.nativeElement, 'lazy-loaded');
    };
    image.onerror = () => {
      this.renderer.removeClass(this.element.nativeElement, 'lazy-loading');
      this.renderer.addClass(this.element.nativeElement, 'lazy-error');
    };
    image.src = src;
  }
}
