; *************************************************************************
; Copyright (c) 1992 Xerox Corporation.  
; All Rights Reserved.  
;
; Use, reproduction, and preparation of derivative works are permitted.
; Any copy of this software or of any derivative work must include the
; above copyright notice of Xerox Corporation, this paragraph and the
; one after it.  Any distribution of this software or derivative works
; must comply with all applicable United States export control laws.
;
; This software is made available AS IS, and XEROX CORPORATION DISCLAIMS
; ALL WARRANTIES, EXPRESS OR IMPLIED, INCLUDING WITHOUT LIMITATION THE
; IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
; PURPOSE, AND NOTWITHSTANDING ANY OTHER PROVISION CONTAINED HEREIN, ANY
; LIABILITY FOR DAMAGES RESULTING FROM THE SOFTWARE OR ITS USE IS
; EXPRESSLY DISCLAIMED, WHETHER ARISING IN CONTRACT, TORT (INCLUDING
; NEGLIGENCE) OR STRICT LIABILITY, EVEN IF XEROX CORPORATION IS ADVISED
; OF THE POSSIBILITY OF SUCH DAMAGES.
; *************************************************************************
;
; port to R6RS -- 2007 Christian Sloma
; 

(library (clos clos) 
  
  (export 
   
   ;; classes
   
   <class>
   <top>
   <object>
   <procedure-class>
   <entity-class>
   <generic>
   <method>


   ;; syntax

   define-class
   define-generic
   define-method  
   
   ;; generic functions
   
   make
   initialize
   allocate-instance
   compute-getter-and-setter
   compute-precedence-list
   compute-slots
   add-method
   compute-apply-generic
   compute-methods
   compute-method-more-specific?
   compute-apply-methods
   print-object
   
   ;; slot access
   
   slot-ref
   slot-set!
   
   ;; introspection
   
   class-of
   class-direct-supers
   class-direct-slots
   class-precedence-list
   class-slots
   class-definition-name
   generic-methods
   method-specializers
   method-procedure
   method-qualifier
   
   ;; helpers
   
   get-arg
   unmangle-class-name
   print-unreadable-object
   print-object-with-slots
   initialize-direct-slots
   
   ) ;; export
  
    (import 
        (only (rnrs) define-syntax syntax-rules syntax-case define ...
            lambda call-with-values datum->syntax quote cons values
            reverse list unsyntax unsyntax-splicing if null? apply
            error car  cdr  not syntax let with-syntax quasisyntax)
        (only (clos bootstrap standard-classes)
            <method> <generic> <entity-class> <procedure-class>
            <object> <top> <class>)
        (only (clos bootstrap generic-functions)
            print-object compute-apply-methods
            compute-method-more-specific?
            compute-methods compute-apply-generic add-method
            compute-slots compute-precedence-list
            compute-getter-and-setter allocate-instance initialize
            make)
        (clos slot-access)
        (clos introspection)
        (clos private compat)
        (clos helpers))

    (define-syntax define-class
        (syntax-rules ()
            ((_ name () slot ...)
                (define-class name (<object>) slot ...))
            ((_ name (super ...) slot ...)
                (define name
                    (make <class>
                        'definition-name 'name
                        'direct-supers   (list super ...) 
                        'direct-slots    '(slot ...))))))
        
    (define-syntax define-generic 
        (syntax-rules ()
            ((_ name)
                (define name
                    (make <generic>
                        'definition-name 'name)))))
        
    (define-syntax define-method 
        (lambda (x) 
            (define (analyse args)
                (let loop ((args args) (qargs '()) (types '()))
                    (syntax-case args ()
                        (((?qarg ?type) . ?args)
                            (loop #'?args (cons #'?qarg qargs) (cons #'?type types)))
                        (?tail
                            (values (reverse qargs) (reverse types) #'?tail)))))
            (define (build kw qualifier generic qargs types tail body)
                (let ((call-next-method (datum->syntax kw 'call-next-method))
                    (next-method?     (datum->syntax kw 'next-method?)))
                        (with-syntax (((?arg ... . ?rest) tail))
                    (let ((rest-args (syntax-case #'?rest ()
                                       (() #''())
                                       (_  #'?rest))))
                        #`(define no-op
                            (add-method #,generic
                                (make <method> 
                                    'specializers (list #,@types)
                                    'qualifier    '#,qualifier
                                    'procedure
                                (lambda (%generic %next-methods #,@qargs ?arg ... . ?rest) 
                                    (let ((#,call-next-method
                                        (lambda ()
                                            (if (null? %next-methods)
                                                (apply error 
                                                    'apply 
                                                    "no next method" 
                                                    %generic 
                                                    #,@qargs ?arg ... #,rest-args)
                                                (apply (car %next-methods)
                                                    %generic
                                                    (cdr %next-methods)
                                                    #,@qargs ?arg ... #,rest-args))))
                                        (next-method?
                                            (not (null? %next-methods)))) 
                                    . #,body)))))))))
            (syntax-case x (quote)
                ((?kw '?qualifier ?generic ?args . ?body)
                    (call-with-values
                        (lambda () 
                            (analyse #'?args))
                        (lambda (qargs types tail)
                            (build #'?kw #'?qualifier #'?generic qargs types tail #'?body))))
                ((?kw ?generic '?qualifier ?args . ?body)
                    #'(?kw '?qualifier ?generic ?args . ?body))
                ((?kw ?generic ?args . ?body)
                    #'(?kw 'primary ?generic ?args . ?body)))))
      
  
  ) 
