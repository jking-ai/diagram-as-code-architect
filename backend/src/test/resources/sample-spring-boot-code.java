@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    public OrderController(OrderService orderService, PaymentService paymentService, NotificationService notificationService) {
        this.orderService = orderService;
        this.paymentService = paymentService;
        this.notificationService = notificationService;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody CreateOrderRequest request) {
        Order order = orderService.create(request);
        paymentService.processPayment(order);
        notificationService.sendConfirmation(order);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Order> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(orderService.findById(id));
    }
}

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;

    public Order create(CreateOrderRequest request) {
        inventoryClient.reserveStock(request.getItems());
        return orderRepository.save(new Order(request));
    }

    public Order findById(UUID id) {
        return orderRepository.findById(id).orElseThrow();
    }
}

@Service
public class PaymentService {
    private final PaymentGatewayClient paymentGateway;

    public void processPayment(Order order) {
        paymentGateway.charge(order.getTotal(), order.getPaymentMethod());
    }
}

@Service
public class NotificationService {
    private final EmailClient emailClient;

    public void sendConfirmation(Order order) {
        emailClient.send(order.getCustomerEmail(), "Order Confirmed", order.getSummary());
    }
}
