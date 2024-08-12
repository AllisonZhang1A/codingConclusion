import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Main {
    // 声明为静态变量，使得所有外卖员线程共享
    private static final PriorityQueue<Order> orderQueue = new PriorityQueue<>(new Comparator<Order>() {
        @Override
        public int compare(Order o1, Order o2) {
            if (o1.priority != o2.priority) {
                return o2.priority - o1.priority; // 优先级高的优先
            } else if (o1.deliveryTime != o2.deliveryTime) {
                return o1.deliveryTime - o2.deliveryTime; // 所需送达时间短的优先
            } else if (o1.createTime != o2.createTime) {
                return o1.createTime - o2.createTime; // 产生时间早的优先
            } else {
                return o1.res_id - o2.res_id; // 餐厅编号小的优先
            }
        }
    });

    // ReentrantLock 和 Condition 用于同步和条件等待
    private static final Lock lock = new ReentrantLock();
    private static final Condition notEmpty = lock.newCondition();


    static class Order {
        int id;           // 订单ID（输入顺序）
        int res_id;       // 餐厅编号
        int createTime;   // 创建时间
        int priority;     // 优先级
        int deliveryTime; // 送达时间

        public Order(int id, int res_id, int createTime, int priority, int deliveryTime) {
            this.id = id;
            this.res_id = res_id;
            this.createTime = createTime;
            this.priority = priority;
            this.deliveryTime = deliveryTime;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt(); // 餐厅数量
        int m = sc.nextInt(); // 外卖员数量
        int p = sc.nextInt(); // 订单数量

        List<Order> orderList = new ArrayList<>();
        // 读订单并加入队列
        for (int i = 0; i < p; i++) {
            int res_id = sc.nextInt();
            int createTime = sc.nextInt();
            int priority = sc.nextInt();
            int deliveryTime = sc.nextInt();
            Order order = new Order(i, res_id, createTime, priority, deliveryTime);
            //将订单加入到订单队列中
            orderList.add(order);
        }
        //订单列表中的订单按照createTime排序
        orderList.sort(Comparator.comparingInt(order -> order.createTime));
        int[] deliveryTimes = new int[p];
        // 启动外卖员线程
        for (int i = 0; i < m; i++) {
            new Thread(() -> {
                int currentTime = 0; // 每个外卖员都有自己的当前时间
                while (true) {
                    Order order;
                    lock.lock();
                    try {
                        while (orderQueue.isEmpty()) {
                            try {
                                notEmpty.await(); // 等待有新订单进入队列
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                        }
                        order = orderQueue.poll();
                    } finally {
                        lock.unlock();
                    }
                    try {
                        // 更新外卖员的当前时间，确保时间是累积的
                        currentTime = Math.max(currentTime, order.createTime) + order.deliveryTime;
                        deliveryTimes[order.id] = currentTime;
                        System.out.println("Order " + order.id + " delivered at time " + currentTime);
                    } catch (Exception e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }).start();
        }

        int currentTime = 0;//当前时间
        int index = 0;
        //模拟时间的推移
        while (index < orderList.size()) {
            lock.lock();
            try {
                // 只在订单的 createTime 等于当前时间时，将订单放入队列
                while (index < orderList.size() && orderList.get(index).createTime == currentTime) {
                    orderQueue.offer(orderList.get(index));
                    notEmpty.signal(); // 唤醒等待的线程
                    index++;
                }
            } finally {
                lock.unlock();
            }
            Thread.sleep(1000L); // 模拟时间的流逝
            currentTime++;
        }
    }
}
